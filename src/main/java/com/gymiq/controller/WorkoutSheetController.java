package com.gymiq.controller;

import java.util.UUID;

import com.gymiq.dto.request.CreateWorkoutSheetRequest;
import com.gymiq.dto.response.WorkoutSheetResponse;
import com.gymiq.dto.response.WorkoutSheetSummaryResponse;
import com.gymiq.service.WorkoutSheetService;
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
@RequestMapping("/api/workout-sheets")
@RequiredArgsConstructor
public class WorkoutSheetController {

    private final WorkoutSheetService workoutSheetService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ResponseEntity<WorkoutSheetResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateWorkoutSheetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workoutSheetService.create(
                request,
                authentication.getName(),
                hasRole(authentication, "ADMIN")));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ResponseEntity<Page<WorkoutSheetSummaryResponse>> findAll(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(workoutSheetService.findAll(pageable));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Page<WorkoutSheetSummaryResponse>> findMine(
            Authentication authentication,
            @RequestParam(defaultValue = "true") boolean onlyActive,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(workoutSheetService.findByAuthenticatedStudent(
                authentication.getName(), onlyActive, pageable));
    }

    @GetMapping("/instructor/me")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<Page<WorkoutSheetSummaryResponse>> findMineAsInstructor(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(workoutSheetService.findByAuthenticatedInstructor(authentication.getName(), pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ResponseEntity<WorkoutSheetResponse> findById(
            @PathVariable UUID id,
            Authentication authentication) {
        return ResponseEntity.ok(workoutSheetService.findById(
                id,
                authentication.getName(),
                hasRole(authentication, "ADMIN")));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ResponseEntity<Page<WorkoutSheetSummaryResponse>> findByStudent(
            @PathVariable UUID studentId,
            Authentication authentication,
            @RequestParam(defaultValue = "true") boolean onlyActive,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(workoutSheetService.findByStudent(
                studentId,
                onlyActive,
                pageable,
                authentication.getName(),
                hasRole(authentication, "ADMIN")));
    }

    @GetMapping("/instructor/{instructorId}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ResponseEntity<Page<WorkoutSheetSummaryResponse>> findByInstructor(
            @PathVariable UUID instructorId,
            Authentication authentication,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(workoutSheetService.findByInstructor(
                instructorId,
                pageable,
                authentication.getName(),
                hasRole(authentication, "ADMIN")));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ResponseEntity<WorkoutSheetResponse> update(
            @PathVariable UUID id,
            Authentication authentication,
            @Valid @RequestBody CreateWorkoutSheetRequest request) {
        return ResponseEntity.ok(workoutSheetService.update(
                id,
                request,
                authentication.getName(),
                hasRole(authentication, "ADMIN")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ResponseEntity<Void> deactivate(
            @PathVariable UUID id,
            Authentication authentication) {
        workoutSheetService.deactivate(
                id,
                authentication.getName(),
                hasRole(authentication, "ADMIN"));
        return ResponseEntity.noContent().build();
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }
}
