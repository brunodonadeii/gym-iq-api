package com.gymiq.controller;

import com.gymiq.dto.request.CadastrarPlanoRequest;
import com.gymiq.dto.response.PlanoResponse;
import com.gymiq.service.PlanoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/planos")
@RequiredArgsConstructor
public class PlanoController {

    private final PlanoService planoService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCAO','ALUNO')")
    public ResponseEntity<List<PlanoResponse>> listarAtivos() {
        return ResponseEntity.ok(planoService.listarAtivos());
    }

    @GetMapping("/todos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PlanoResponse>> listarTodos() {
        return ResponseEntity.ok(planoService.listarTodos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCAO','ALUNO')")
    public ResponseEntity<PlanoResponse> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(planoService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlanoResponse> criar(
            @Valid @RequestBody CadastrarPlanoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planoService.criar(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlanoResponse> atualizar(
            @PathVariable Integer id,
            @Valid @RequestBody CadastrarPlanoRequest request) {
        return ResponseEntity.ok(planoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desativar(@PathVariable Integer id) {
        planoService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
