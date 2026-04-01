package com.gymiq.controller;

import com.gymiq.dto.request.CadastrarAlunoRequest;
import com.gymiq.dto.response.AlunoResponse;
import com.gymiq.service.AlunoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/alunos")
@RequiredArgsConstructor
public class AlunoController {

    private final AlunoService alunoService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCAO')")
    public ResponseEntity<AlunoResponse> criar(
            @Valid @RequestBody CadastrarAlunoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(alunoService.criar(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCAO','INSTRUTOR')")
    public ResponseEntity<List<AlunoResponse>> listar() {
        return ResponseEntity.ok(alunoService.listarTodos());
    }

    @GetMapping("/busca")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCAO','INSTRUTOR')")
    public ResponseEntity<List<AlunoResponse>> buscar(@RequestParam String q) {
        return ResponseEntity.ok(alunoService.buscar(q));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCAO','INSTRUTOR','ALUNO')")
    public ResponseEntity<AlunoResponse> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(alunoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCAO')")
    public ResponseEntity<AlunoResponse> atualizar(
            @PathVariable Integer id,
            @Valid @RequestBody CadastrarAlunoRequest request) {
        return ResponseEntity.ok(alunoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> inativar(@PathVariable Integer id) {
        alunoService.inativar(id);
        return ResponseEntity.noContent().build();
    }
}
