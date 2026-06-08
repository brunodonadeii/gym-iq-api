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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

        UserResponse response = userService.createAdministrativeUser(request);

        assertThat(response.getUserId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000031"));
        assertThat(response.getRole()).isEqualTo("RECEPTION");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createAdministrativeUserShouldRejectStudentRole() {
        CreateUserRequest request = createUserRequest(User.Role.STUDENT);

        assertThatThrownBy(() -> userService.createAdministrativeUser(request))
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
    void deleteAdministrativeUserShouldRemoveOnlyAdministrativeUser() {
        User user = TestDataFactory.activeAdminUser();

        when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));

        userService.deleteAdministrativeUser(user.getUserId());

        verify(userRepository).delete(user);
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
        request.setLgpdAccepted(true);
        return request;
    }
}
