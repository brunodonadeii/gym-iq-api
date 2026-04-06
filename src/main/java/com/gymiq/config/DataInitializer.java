package com.gymiq.config;

import com.gymiq.entity.User;
import com.gymiq.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        criarAdminSeNaoExistir();
    }

    private void criarAdminSeNaoExistir() {
        if (userRepository.existsByEmail("admin@gymiq.com")) {
            log.info("Usuário admin já existe — seed ignorado.");
            return;
        }

        User admin = User.builder()
                .name("Administrador GymIQ")
                .email("admin@gymiq.com")
                .passwordHash(passwordEncoder.encode("gymiq@2026"))
                .role(User.Role.ADMIN)
                .active(true)
                .lgpdAccepted(true)
                .build();

        userRepository.save(admin);

        log.info("=================================================");
        log.info("  Usuário admin criado com sucesso!");
        log.info("  Email : admin@gymiq.com");
        log.info("  Senha : gymiq@2026");
        log.info("=================================================");
    }
}