package com.gymiq.controller;

import com.gymiq.dto.request.MatricularAlunoRequest;
import com.gymiq.dto.response.MatriculaResponse;
import com.gymiq.entity.Matricula.StatusMatricula;
import com.gymiq.service.MatriculaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/matriculas")
@RequiredArgsConstructor
public class MatriculaController {

    private final MatriculaService matriculaService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCAO')")
    public ResponseEntity<MatriculaResponse> matricular(
            @Valid @RequestBody MatricularAlunoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(matriculaService.matricular(request));
    }

    @GetMapping("/aluno/{idAluno}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCAO')")
    public ResponseEntity<List<MatriculaResponse>> listarPorAluno(
            @PathVariable Integer idAluno) {
        return ResponseEntity.ok(matriculaService.listarPorAluno(idAluno));
    }

    @GetMapping("/aluno/{idAluno}/ativa")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCAO','ALUNO')")
    public ResponseEntity<MatriculaResponse> buscarAtivaDoAluno(
            @PathVariable Integer idAluno) {
        return ResponseEntity.ok(matriculaService.buscarAtivaDoAluno(idAluno));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCAO')")
    public ResponseEntity<MatriculaResponse> alterarStatus(
            @PathVariable Integer id,
            @RequestParam StatusMatricula novoStatus) {
        return ResponseEntity.ok(matriculaService.alterarStatus(id, novoStatus));
    }

    @PostMapping("/{id}/renovar")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCAO')")
    public ResponseEntity<MatriculaResponse> renovar(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer idNovoPlano) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(matriculaService.renovar(id, idNovoPlano));
    }
}
