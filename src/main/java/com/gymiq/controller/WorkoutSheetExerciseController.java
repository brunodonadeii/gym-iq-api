package com.gymiq.controller;

import com.gymiq.dto.request.CreateWorkoutSheetExerciseRequest;
import com.gymiq.dto.response.WorkoutSheetExerciseResponse;
import com.gymiq.service.WorkoutSheetExerciseService;
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
@RequiredArgsConstructor
public class WorkoutSheetExerciseController {

    private final WorkoutSheetExerciseService workoutSheetExerciseService;

    @PostMapping("/api/workout-sheets/{workoutSheetId}/exercises")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ResponseEntity<WorkoutSheetExerciseResponse> addExercise(
            @PathVariable Integer workoutSheetId,
            Authentication authentication,
            @Valid @RequestBody CreateWorkoutSheetExerciseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workoutSheetExerciseService.addExercise(
                        workoutSheetId,
                        request,
                        authentication.getName(),
                        hasRole(authentication, "ADMIN")));
    }

    @GetMapping("/api/workout-sheets/{workoutSheetId}/exercises")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR','STUDENT')")
    public ResponseEntity<Page<WorkoutSheetExerciseResponse>> findByWorkoutSheet(
            @PathVariable Integer workoutSheetId,
            Authentication authentication,
            @PageableDefault(size = 10, sort = "executionOrder", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(workoutSheetExerciseService.findByWorkoutSheet(
                workoutSheetId,
                pageable,
                authentication.getName(),
                hasRole(authentication, "ADMIN"),
                hasRole(authentication, "INSTRUCTOR"),
                hasRole(authentication, "STUDENT")));
    }

    @PutMapping("/api/workout-sheet-exercises/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ResponseEntity<WorkoutSheetExerciseResponse> update(
            @PathVariable Integer id,
            Authentication authentication,
            @Valid @RequestBody CreateWorkoutSheetExerciseRequest request) {
        return ResponseEntity.ok(workoutSheetExerciseService.update(
                id,
                request,
                authentication.getName(),
                hasRole(authentication, "ADMIN")));
    }

    @DeleteMapping("/api/workout-sheet-exercises/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ResponseEntity<Void> delete(
            @PathVariable Integer id,
            Authentication authentication) {
        workoutSheetExerciseService.delete(
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
