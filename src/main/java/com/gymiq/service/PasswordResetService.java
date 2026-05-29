package com.gymiq.service;

import com.gymiq.dto.request.ForgotPasswordRequest;
import com.gymiq.dto.request.ResetPasswordRequest;
import com.gymiq.dto.response.MessageResponse;
import com.gymiq.entity.PasswordResetToken;
import com.gymiq.entity.User;
import com.gymiq.exception.BusinessException;
import com.gymiq.repository.PasswordResetTokenRepository;
import com.gymiq.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int TOKEN_BYTES = 32;
    private static final String GENERIC_RESET_MESSAGE =
            "Se o e-mail estiver cadastrado, enviaremos instrucoes para redefinir a senha.";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${gymiq.password-reset.frontend-url}")
    private String resetPasswordFrontendUrl;

    @Value("${gymiq.password-reset.expiration-minutes:30}")
    private long expirationMinutes;

    @Value("${gymiq.password-reset.resend-cooldown-minutes:15}")
    private long resendCooldownMinutes;

    @Value("${gymiq.mail.from:}")
    private String mailFrom;

    @PostConstruct
    void validateConfiguration() {
        if (resetPasswordFrontendUrl == null || resetPasswordFrontendUrl.isBlank()) {
            throw new IllegalStateException("gymiq.password-reset.frontend-url deve ser configurado");
        }
        if (expirationMinutes <= 0) {
            throw new IllegalStateException("gymiq.password-reset.expiration-minutes deve ser maior que zero");
        }
        if (resendCooldownMinutes <= 0) {
            throw new IllegalStateException("gymiq.password-reset.resend-cooldown-minutes deve ser maior que zero");
        }
    }

    @Transactional
    public MessageResponse requestPasswordReset(ForgotPasswordRequest request) {
        String email = request.getEmail().trim();
        LocalDateTime now = LocalDateTime.now();

        deleteExpiredTokens(now);

        userRepository.findByEmailIgnoreCase(email)
                .filter(user -> Boolean.TRUE.equals(user.getActive()))
                .ifPresent(user -> createTokenAndSendEmail(user, now));

        return buildMessage(GENERIC_RESET_MESSAGE);
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        LocalDateTime now = LocalDateTime.now();
        deleteExpiredTokens(now);

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenHashAndUsedFalse(hashToken(request.getToken().trim()))
                .orElseThrow(() -> new BusinessException("Token invalido ou expirado"));

        if (resetToken.isExpired(now)) {
            throw new BusinessException("Token invalido ou expirado");
        }

        User user = resetToken.getUser();

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BusinessException("Usuario inativo");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException("Nova senha deve ser diferente da senha atual");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        markTokenAsUsed(resetToken, now);
        invalidateOtherOpenTokens(user, resetToken, now);

        log.info("Senha redefinida com sucesso para usuario id={}", user.getUserId());

        return buildMessage("Senha alterada com sucesso.");
    }

    private void createTokenAndSendEmail(User user, LocalDateTime now) {
        if (hasRecentOpenResetRequest(user, now)) {
            log.info("Solicitacao de redefinicao ignorada por cooldown para usuario id={}", user.getUserId());
            return;
        }

        String rawToken = generateToken();

        invalidateOpenTokens(user, now);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash(hashToken(rawToken))
                .expiresAt(now.plusMinutes(expirationMinutes))
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);
        sendResetEmail(user, rawToken);
        log.info("Token de redefinicao de senha criado para usuario id={}", user.getUserId());
    }

    private boolean hasRecentOpenResetRequest(User user, LocalDateTime now) {
        return passwordResetTokenRepository
                .findTopByUserUserIdAndUsedFalseOrderByCreatedAtDesc(user.getUserId())
                .map(PasswordResetToken::getCreatedAt)
                .filter(createdAt -> createdAt != null)
                .map(createdAt -> createdAt.isAfter(now.minusMinutes(resendCooldownMinutes)))
                .orElse(false);
    }

    private void sendResetEmail(User user, String rawToken) {
        String resetLink = buildResetLink(rawToken);

        SimpleMailMessage message = new SimpleMailMessage();
        if (mailFrom != null && !mailFrom.isBlank()) {
            message.setFrom(mailFrom);
        }
        message.setTo(user.getEmail());
        message.setSubject("Redefinicao de senha - GymIQ");
        message.setText("""
                Ola, %s.

                Recebemos uma solicitacao para redefinir sua senha no GymIQ.

                Acesse o link abaixo para cadastrar uma nova senha:
                %s

                Este link expira em %d minutos.
                Caso voce nao tenha solicitado esta alteracao, ignore este e-mail.
                """.formatted(user.getName(), resetLink, expirationMinutes));

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            log.error("Falha ao enviar e-mail de redefinicao de senha para usuario id={}", user.getUserId(), ex);
            throw new BusinessException("Nao foi possivel enviar o e-mail de redefinicao de senha");
        }
    }

    private String buildResetLink(String rawToken) {
        return UriComponentsBuilder.fromUriString(resetPasswordFrontendUrl)
                .queryParam("token", rawToken)
                .build()
                .toUriString();
    }

    private void deleteExpiredTokens(LocalDateTime now) {
        long deletedTokens = passwordResetTokenRepository.deleteByExpiresAtBefore(now);
        if (deletedTokens > 0) {
            log.info("Tokens expirados de redefinicao de senha removidos: {}", deletedTokens);
        }
    }

    private void invalidateOpenTokens(User user, LocalDateTime now) {
        passwordResetTokenRepository.findByUserUserIdAndUsedFalse(user.getUserId())
                .forEach(token -> markTokenAsUsed(token, now));
    }

    private void invalidateOtherOpenTokens(User user, PasswordResetToken usedToken, LocalDateTime now) {
        passwordResetTokenRepository.findByUserUserIdAndUsedFalse(user.getUserId()).stream()
                .filter(token -> !token.getPasswordResetTokenId().equals(usedToken.getPasswordResetTokenId()))
                .forEach(token -> markTokenAsUsed(token, now));
    }

    private void markTokenAsUsed(PasswordResetToken token, LocalDateTime usedAt) {
        token.setUsed(true);
        token.setUsedAt(usedAt);
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 nao esta disponivel", ex);
        }
    }

    private MessageResponse buildMessage(String message) {
        return MessageResponse.builder()
                .message(message)
                .build();
    }
}
