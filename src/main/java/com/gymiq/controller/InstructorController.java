package com.gymiq.controller;

import com.gymiq.dto.request.CreateInstructorRequest;
import com.gymiq.dto.response.InstructorResponse;
import com.gymiq.service.InstructorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Page<InstructorResponse>> findAll(
            @PageableDefault(size = 10, sort = "user.name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(instructorService.findAll(pageable));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<Page<InstructorResponse>> search(
            @RequestParam String q,
            @PageableDefault(size = 10, sort = "user.name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(instructorService.search(q, pageable));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<InstructorResponse> findMe(Authentication authentication) {
        return ResponseEntity.ok(instructorService.findByAuthenticatedEmail(authentication.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
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
