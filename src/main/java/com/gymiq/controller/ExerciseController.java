package com.gymiq.controller;

import com.gymiq.dto.request.CreateExerciseRequest;
import com.gymiq.dto.response.ExerciseResponse;
import com.gymiq.service.ExerciseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exercises")
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseService exerciseService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ResponseEntity<ExerciseResponse> create(
            @Valid @RequestBody CreateExerciseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(exerciseService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION','INSTRUCTOR','STUDENT')")
    public ResponseEntity<List<ExerciseResponse>> findActive() {
        return ResponseEntity.ok(exerciseService.findActive());
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ResponseEntity<List<ExerciseResponse>> findAll() {
        return ResponseEntity.ok(exerciseService.findAll());
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION','INSTRUCTOR','STUDENT')")
    public ResponseEntity<List<ExerciseResponse>> search(@RequestParam String q) {
        return ResponseEntity.ok(exerciseService.search(q));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION','INSTRUCTOR','STUDENT')")
    public ResponseEntity<ExerciseResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(exerciseService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ResponseEntity<ExerciseResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody CreateExerciseRequest request) {
        return ResponseEntity.ok(exerciseService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ResponseEntity<Void> deactivate(@PathVariable Integer id) {
        exerciseService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
