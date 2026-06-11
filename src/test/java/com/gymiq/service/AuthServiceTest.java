package com.gymiq.service;

import com.gymiq.dto.request.LoginRequest;
import com.gymiq.dto.response.AuthResponse;
import com.gymiq.entity.User;
import com.gymiq.repository.UserRepository;
import com.gymiq.security.JwtUtil;
import com.gymiq.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PersonalDataProtectionService personalDataProtectionService;

    @Mock
    private StudentContractService studentContractService;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginShouldAuthenticateAndReturnTokenPayload() {
        LoginRequest request = new LoginRequest();
        request.setEmail("ana@gymiq.com");
        request.setPassword("secret123");

        User user = TestDataFactory.activeStudentUser();

        when(personalDataProtectionService.emailHash(request.getEmail())).thenReturn("email-hash");
        when(userRepository.findByEmailHash("email-hash")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getUserId()))
                .thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getType()).isEqualTo("Bearer");
        assertThat(response.getRole()).isEqualTo("STUDENT");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

}
