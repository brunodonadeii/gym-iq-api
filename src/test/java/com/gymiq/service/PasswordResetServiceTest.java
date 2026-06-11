package com.gymiq.service;

import com.gymiq.dto.request.ForgotPasswordRequest;
import com.gymiq.dto.request.ResetPasswordRequest;
import com.gymiq.dto.response.MessageResponse;
import com.gymiq.entity.PasswordResetToken;
import com.gymiq.entity.User;
import com.gymiq.exception.BusinessException;
import com.gymiq.repository.PasswordResetTokenRepository;
import com.gymiq.repository.UserRepository;
import com.gymiq.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PersonalDataProtectionService personalDataProtectionService;

    @Mock
    private JavaMailSender mailSender;

    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(
                userRepository,
                passwordResetTokenRepository,
                passwordEncoder,
                personalDataProtectionService,
                mailSender);

        ReflectionTestUtils.setField(passwordResetService, "resetPasswordFrontendUrl", "https://gymiq.app/reset-password");
        ReflectionTestUtils.setField(passwordResetService, "expirationMinutes", 30L);
        ReflectionTestUtils.setField(passwordResetService, "resendCooldownMinutes", 15L);
        ReflectionTestUtils.setField(passwordResetService, "mailFrom", "suporte@gymiq.com");
    }

    @Test
    void validateConfigurationShouldRejectMissingFrontendUrl() {
        ReflectionTestUtils.setField(passwordResetService, "resetPasswordFrontendUrl", "");

        assertThatThrownBy(() -> passwordResetService.validateConfiguration())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("frontend-url");
    }

    @Test
    void requestPasswordResetShouldReturnGenericMessageWhenEmailDoesNotExist() {
        ForgotPasswordRequest request = forgotPasswordRequest(" ana@gymiq.com ");

        when(personalDataProtectionService.emailHash("ana@gymiq.com")).thenReturn("email-hash");
        when(passwordResetTokenRepository.deleteByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(2L);
        when(userRepository.findByEmailHash("email-hash")).thenReturn(Optional.empty());

        MessageResponse response = passwordResetService.requestPasswordReset(request);

        assertThat(response.getMessage()).contains("Se o e-mail estiver cadastrado");
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void requestPasswordResetShouldCreateTokenAndSendEmailForActiveUser() {
        User user = TestDataFactory.activeStudentUser();
        PasswordResetToken oldToken = openToken(user, "old-hash", LocalDateTime.now().minusMinutes(20));
        ForgotPasswordRequest request = forgotPasswordRequest(user.getEmail());

        when(personalDataProtectionService.emailHash(user.getEmail())).thenReturn("email-hash");
        when(userRepository.findByEmailHash("email-hash")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findTopByUserUserIdAndUsedFalseOrderByCreatedAtDesc(user.getUserId()))
                .thenReturn(Optional.empty());
        when(passwordResetTokenRepository.findByUserUserIdAndUsedFalse(user.getUserId())).thenReturn(List.of(oldToken));

        passwordResetService.requestPasswordReset(request);

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        verify(mailSender).send(mailCaptor.capture());

        assertThat(oldToken.getUsed()).isTrue();
        assertThat(tokenCaptor.getValue().getUser()).isSameAs(user);
        assertThat(tokenCaptor.getValue().getTokenHash()).hasSize(64);
        assertThat(tokenCaptor.getValue().getUsed()).isFalse();
        assertThat(mailCaptor.getValue().getFrom()).isEqualTo("suporte@gymiq.com");
        assertThat(mailCaptor.getValue().getTo()).containsExactly(user.getEmail());
        assertThat(mailCaptor.getValue().getText()).contains("https://gymiq.app/reset-password?token=");
    }

    @Test
    void requestPasswordResetShouldRespectResendCooldown() {
        User user = TestDataFactory.activeStudentUser();
        PasswordResetToken recentToken = openToken(user, "recent-hash", LocalDateTime.now());

        when(personalDataProtectionService.emailHash(user.getEmail())).thenReturn("email-hash");
        when(userRepository.findByEmailHash("email-hash")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findTopByUserUserIdAndUsedFalseOrderByCreatedAtDesc(user.getUserId()))
                .thenReturn(Optional.of(recentToken));

        passwordResetService.requestPasswordReset(forgotPasswordRequest(user.getEmail()));

        verify(passwordResetTokenRepository, never()).save(any());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void requestPasswordResetShouldThrowBusinessExceptionWhenMailFails() {
        User user = TestDataFactory.activeStudentUser();

        when(personalDataProtectionService.emailHash(user.getEmail())).thenReturn("email-hash");
        when(userRepository.findByEmailHash("email-hash")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findTopByUserUserIdAndUsedFalseOrderByCreatedAtDesc(user.getUserId()))
                .thenReturn(Optional.empty());
        when(passwordResetTokenRepository.findByUserUserIdAndUsedFalse(user.getUserId())).thenReturn(List.of());
        org.mockito.Mockito.doThrow(new MailSendException("smtp offline"))
                .when(mailSender)
                .send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> passwordResetService.requestPasswordReset(forgotPasswordRequest(user.getEmail())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("e-mail");
    }

    @Test
    void resetPasswordShouldChangePasswordAndInvalidateOtherTokens() {
        User user = TestDataFactory.activeStudentUser();
        PasswordResetToken token = openToken(user, hashToken("reset-token"), LocalDateTime.now().minusMinutes(2));
        token.setPasswordResetTokenId(UUID.fromString("00000000-0000-0000-0000-000000000201"));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(20));
        PasswordResetToken otherToken = openToken(user, "other-hash", LocalDateTime.now().minusMinutes(1));
        otherToken.setPasswordResetTokenId(UUID.fromString("00000000-0000-0000-0000-000000000202"));

        when(passwordResetTokenRepository.findByTokenHashAndUsedFalse(hashToken("reset-token")))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.matches("novaSenha123", user.getPasswordHash())).thenReturn(false);
        when(passwordEncoder.encode("novaSenha123")).thenReturn("encoded-new-password");
        when(passwordResetTokenRepository.findByUserUserIdAndUsedFalse(user.getUserId()))
                .thenReturn(List.of(token, otherToken));

        MessageResponse response = passwordResetService.resetPassword(resetPasswordRequest("reset-token", "novaSenha123"));

        assertThat(response.getMessage()).isEqualTo("Senha alterada com sucesso.");
        assertThat(user.getPasswordHash()).isEqualTo("encoded-new-password");
        assertThat(token.getUsed()).isTrue();
        assertThat(otherToken.getUsed()).isTrue();
        assertThat(token.getUsedAt()).isNotNull();
        assertThat(otherToken.getUsedAt()).isNotNull();
    }

    @Test
    void resetPasswordShouldRejectExpiredOrInvalidToken() {
        when(passwordResetTokenRepository.findByTokenHashAndUsedFalse(hashToken("missing-token")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.resetPassword(resetPasswordRequest("missing-token", "novaSenha123")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Token");
    }

    @Test
    void resetPasswordShouldRejectSamePassword() {
        User user = TestDataFactory.activeStudentUser();
        PasswordResetToken token = openToken(user, hashToken("reset-token"), LocalDateTime.now().minusMinutes(2));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(20));

        when(passwordResetTokenRepository.findByTokenHashAndUsedFalse(hashToken("reset-token")))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.matches("senhaAtual", user.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> passwordResetService.resetPassword(resetPasswordRequest("reset-token", "senhaAtual")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("diferente");
    }

    private ForgotPasswordRequest forgotPasswordRequest(String email) {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(email);
        return request;
    }

    private ResetPasswordRequest resetPasswordRequest(String token, String password) {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(token);
        request.setNewPassword(password);
        return request;
    }

    private PasswordResetToken openToken(User user, String tokenHash, LocalDateTime createdAt) {
        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .build();
        token.setPasswordResetTokenId(UUID.randomUUID());
        token.setCreatedAt(createdAt);
        return token;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
