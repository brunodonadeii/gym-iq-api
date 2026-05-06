package com.gymiq.controller;

import com.gymiq.dto.request.CreateWorkoutSheetRequest;
import com.gymiq.dto.response.WorkoutSheetResponse;
import com.gymiq.service.WorkoutSheetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workout-sheets")
@RequiredArgsConstructor
public class WorkoutSheetController {

    private final WorkoutSheetService workoutSheetService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ResponseEntity<WorkoutSheetResponse> create(
            @Valid @RequestBody CreateWorkoutSheetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workoutSheetService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ResponseEntity<List<WorkoutSheetResponse>> findAll() {
        return ResponseEntity.ok(workoutSheetService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR','STUDENT')")
    public ResponseEntity<WorkoutSheetResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(workoutSheetService.findById(id));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR','STUDENT')")
    public ResponseEntity<List<WorkoutSheetResponse>> findByStudent(
            @PathVariable Integer studentId,
            @RequestParam(defaultValue = "true") boolean onlyActive) {
        return ResponseEntity.ok(workoutSheetService.findByStudent(studentId, onlyActive));
    }

    @GetMapping("/instructor/{instructorId}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ResponseEntity<List<WorkoutSheetResponse>> findByInstructor(
            @PathVariable Integer instructorId) {
        return ResponseEntity.ok(workoutSheetService.findByInstructor(instructorId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ResponseEntity<WorkoutSheetResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody CreateWorkoutSheetRequest request) {
        return ResponseEntity.ok(workoutSheetService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ResponseEntity<Void> deactivate(@PathVariable Integer id) {
        workoutSheetService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
