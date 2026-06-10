package com.gymiq.service;

import com.gymiq.dto.request.CreateUserRequest;
import com.gymiq.dto.request.UpdateUserRequest;
import com.gymiq.dto.response.UserResponse;
import com.gymiq.entity.User;
import com.gymiq.exception.BusinessException;
import com.gymiq.repository.UserRepository;
import com.gymiq.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PersonalDataProtectionService personalDataProtectionService;

    @InjectMocks
    private UserService userService;

    @Test
    void createAdministrativeUserShouldPersistAdminOrReceptionOnly() {
        CreateUserRequest request = createUserRequest(User.Role.RECEPTION);

        when(personalDataProtectionService.emailHash(request.getEmail())).thenReturn("email-hash");
        when(userRepository.existsByEmailHash("email-hash")).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000031"));
            return user;
        });

        UserResponse response = userService.createAdministrativeUser(request, authentication("ROLE_ADMIN"));

        assertThat(response.getUserId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000031"));
        assertThat(response.getRole()).isEqualTo("RECEPTION");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createAdministrativeUserShouldRejectStudentRole() {
        CreateUserRequest request = createUserRequest(User.Role.STUDENT);

        assertThatThrownBy(() -> userService.createAdministrativeUser(request, authentication("ROLE_ADMIN")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("rotas");
    }

    @Test
    void updateAdministrativeUserShouldChangeEmailRoleAndLgpd() {
        User user = TestDataFactory.activeAdminUser();
        UpdateUserRequest request = updateUserRequest();

        when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(personalDataProtectionService.emailHash(request.getEmail())).thenReturn("updated-email-hash");
        when(userRepository.findByEmailHash("updated-email-hash")).thenReturn(Optional.empty());

        UserResponse response = userService.updateAdministrativeUser(user.getUserId(), request);

        assertThat(response.getName()).isEqualTo("Recepcao GymIQ");
        assertThat(response.getRole()).isEqualTo("RECEPTION");
        assertThat(user.getEmailHash()).isEqualTo("updated-email-hash");
        verify(userRepository).save(user);
    }

    @Test
    void updateAdministrativeUserShouldRejectSeededAdminIdentityChange() {
        User seededAdmin = administrativeUser(
                UUID.fromString("00000000-0000-0000-0000-000000000037"),
                "admin@gymiq.com",
                "seeded-admin-email-hash",
                User.Role.ADMIN);
        UpdateUserRequest request = updateUserRequest();

        when(userRepository.findById(seededAdmin.getUserId())).thenReturn(Optional.of(seededAdmin));
        when(personalDataProtectionService.emailHash(request.getEmail())).thenReturn("updated-email-hash");
        when(personalDataProtectionService.emailHash("admin@gymiq.com")).thenReturn("seeded-admin-email-hash");

        assertThatThrownBy(() -> userService.updateAdministrativeUser(seededAdmin.getUserId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("administrador padrão");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteAdministrativeUserShouldRemoveOnlyAdministrativeUser() {
        User user = administrativeUser(
                UUID.fromString("00000000-0000-0000-0000-000000000032"),
                "recepcao@gymiq.com",
                "target-email-hash",
                User.Role.RECEPTION);
        User authenticatedUser = administrativeUser(
                UUID.fromString("00000000-0000-0000-0000-000000000033"),
                "admin2@gymiq.com",
                "authenticated-email-hash",
                User.Role.ADMIN);

        when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(personalDataProtectionService.emailHash("admin2@gymiq.com")).thenReturn("authenticated-email-hash");
        when(userRepository.findByEmailHash("authenticated-email-hash")).thenReturn(Optional.of(authenticatedUser));
        when(personalDataProtectionService.emailHash("admin@gymiq.com")).thenReturn("seeded-admin-email-hash");

        userService.deleteAdministrativeUser(user.getUserId(), "admin2@gymiq.com");

        verify(userRepository).delete(user);
    }

    @Test
    void deleteAdministrativeUserShouldRejectSelfDeletion() {
        User user = administrativeUser(
                UUID.fromString("00000000-0000-0000-0000-000000000034"),
                "admin2@gymiq.com",
                "authenticated-email-hash",
                User.Role.ADMIN);

        when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(personalDataProtectionService.emailHash("admin2@gymiq.com")).thenReturn("authenticated-email-hash");
        when(userRepository.findByEmailHash("authenticated-email-hash")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.deleteAdministrativeUser(user.getUserId(), "admin2@gymiq.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("próprio usuário administrador");

        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void deleteAdministrativeUserShouldRejectSeededAdminDeletion() {
        User seededAdmin = administrativeUser(
                UUID.fromString("00000000-0000-0000-0000-000000000035"),
                "admin@gymiq.com",
                "seeded-admin-email-hash",
                User.Role.ADMIN);
        User authenticatedUser = administrativeUser(
                UUID.fromString("00000000-0000-0000-0000-000000000036"),
                "admin2@gymiq.com",
                "authenticated-email-hash",
                User.Role.ADMIN);

        when(userRepository.findById(seededAdmin.getUserId())).thenReturn(Optional.of(seededAdmin));
        when(personalDataProtectionService.emailHash("admin2@gymiq.com")).thenReturn("authenticated-email-hash");
        when(userRepository.findByEmailHash("authenticated-email-hash")).thenReturn(Optional.of(authenticatedUser));
        when(personalDataProtectionService.emailHash("admin@gymiq.com")).thenReturn("seeded-admin-email-hash");

        assertThatThrownBy(() -> userService.deleteAdministrativeUser(seededAdmin.getUserId(), "admin2@gymiq.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("administrador padrão");

        verify(userRepository, never()).delete(any(User.class));
    }

    private User administrativeUser(UUID userId, String email, String emailHash, User.Role role) {
        User user = User.builder()
                .name("Usuario Administrativo")
                .email(email)
                .emailHash(emailHash)
                .passwordHash("encoded-password")
                .role(role)
                .active(true)
                .lgpdAccepted(true)
                .build();
        user.setUserId(userId);
        return user;
    }

    private CreateUserRequest createUserRequest(User.Role role) {
        CreateUserRequest request = new CreateUserRequest();
        request.setName("Recepcao GymIQ");
        request.setEmail("recepcao@gymiq.com");
        request.setPassword("secret123");
        request.setRole(role);
        request.setLgpdAccepted(true);
        return request;
    }

    private UpdateUserRequest updateUserRequest() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("Recepcao GymIQ");
        request.setEmail("recepcao@gymiq.com");
        request.setRole(User.Role.RECEPTION);
        return request;
    }

    private Authentication authentication(String role) {
        return new TestingAuthenticationToken(
                "admin@gymiq.com",
                null,
                java.util.List.of(new SimpleGrantedAuthority(role)));
    }
}
