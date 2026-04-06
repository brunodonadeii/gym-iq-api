package com.gymiq.controller;

import com.gymiq.dto.request.CreateStudentRequest;
import com.gymiq.dto.request.LoginRequest;
import com.gymiq.dto.response.StudentResponse;
import com.gymiq.dto.response.AuthResponse;
import com.gymiq.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<StudentResponse> register(
            @Valid @RequestBody CreateStudentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerStudent(request));
    }

    @PostMapping("/lgpd/{userId}")
    @PreAuthorize("authentication.name == @userRepository.findById(#userId).orElseThrow().email" +
            " or hasRole('ADMIN')")
    public ResponseEntity<Void> acceptLgpd(@PathVariable Integer userId) {
        authService.registerLgpdAcceptance(userId);
        return ResponseEntity.noContent().build();
    }
}