package com.gymiq.controller;

import com.gymiq.dto.request.CreateWorkoutBlockRequest;
import com.gymiq.dto.response.WorkoutBlockResponse;
import com.gymiq.dto.response.WorkoutBlockSummaryResponse;
import com.gymiq.service.WorkoutBlockService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class WorkoutBlockController {

    private final WorkoutBlockService workoutBlockService;

    @PostMapping("/api/workout-sheets/{workoutSheetId}/blocks")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ResponseEntity<WorkoutBlockResponse> create(
            @PathVariable UUID workoutSheetId,
            Authentication authentication,
            @Valid @RequestBody CreateWorkoutBlockRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workoutBlockService.create(
                workoutSheetId,
                request,
                authentication.getName(),
                hasRole(authentication, "ADMIN")));
    }

    @GetMapping("/api/workout-sheets/{workoutSheetId}/blocks")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR','STUDENT')")
    public ResponseEntity<Page<WorkoutBlockSummaryResponse>> findByWorkoutSheet(
            @PathVariable UUID workoutSheetId,
            Authentication authentication,
            @PageableDefault(size = 10, sort = "executionOrder", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(workoutBlockService.findByWorkoutSheet(
                workoutSheetId,
                pageable,
                authentication.getName(),
                hasRole(authentication, "ADMIN"),
                hasRole(authentication, "INSTRUCTOR"),
                hasRole(authentication, "STUDENT")));
    }

    @GetMapping("/api/workout-blocks/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR','STUDENT')")
    public ResponseEntity<WorkoutBlockResponse> findById(
            @PathVariable UUID id,
            Authentication authentication) {
        return ResponseEntity.ok(workoutBlockService.findById(
                id,
                authentication.getName(),
                hasRole(authentication, "ADMIN"),
                hasRole(authentication, "INSTRUCTOR"),
                hasRole(authentication, "STUDENT")));
    }

    @PutMapping("/api/workout-blocks/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ResponseEntity<WorkoutBlockResponse> update(
            @PathVariable UUID id,
            Authentication authentication,
            @Valid @RequestBody CreateWorkoutBlockRequest request) {
        return ResponseEntity.ok(workoutBlockService.update(
                id,
                request,
                authentication.getName(),
                hasRole(authentication, "ADMIN")));
    }

    @DeleteMapping("/api/workout-blocks/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            Authentication authentication) {
        workoutBlockService.delete(
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
