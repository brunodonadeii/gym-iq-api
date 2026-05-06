package com.gymiq.controller;

import com.gymiq.dto.request.CreateInstructorRequest;
import com.gymiq.dto.response.InstructorResponse;
import com.gymiq.service.InstructorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/instructors")
@RequiredArgsConstructor
public class InstructorController {

    private final InstructorService instructorService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InstructorResponse> create(
            @Valid @RequestBody CreateInstructorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(instructorService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<List<InstructorResponse>> findAll() {
        return ResponseEntity.ok(instructorService.findAll());
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<List<InstructorResponse>> search(@RequestParam String q) {
        return ResponseEntity.ok(instructorService.search(q));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION','INSTRUCTOR')")
    public ResponseEntity<InstructorResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(instructorService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InstructorResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody CreateInstructorRequest request) {
        return ResponseEntity.ok(instructorService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Integer id) {
        instructorService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
