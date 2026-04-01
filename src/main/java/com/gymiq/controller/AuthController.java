package com.gymiq.controller;

import com.gymiq.dto.request.CadastrarAlunoRequest;
import com.gymiq.dto.request.LoginRequest;
import com.gymiq.dto.response.AlunoResponse;
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


    @PostMapping("/registro")
    public ResponseEntity<AlunoResponse> registro(
            @Valid @RequestBody CadastrarAlunoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registrarAluno(request));
    }


    @PostMapping("/lgpd/{idUsuario}")
    @PreAuthorize("authentication.name == @usuarioRepository.findById(#idUsuario).orElseThrow().email" +
                  " or hasRole('ADMIN')")
    public ResponseEntity<Void> aceitarLgpd(@PathVariable Integer idUsuario) {
        authService.registrarAceiteLgpd(idUsuario);
        return ResponseEntity.noContent().build();
    }
}
