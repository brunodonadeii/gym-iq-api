package com.gymiq.controller;

import com.gymiq.dto.request.CreatePlanRequest;
import com.gymiq.dto.response.PlanResponse;
import com.gymiq.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION','STUDENT')")
    public ResponseEntity<Page<PlanResponse>> findActive(
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(planService.findActive(pageable));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<PlanResponse>> findAll(
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(planService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION','STUDENT')")
    public ResponseEntity<PlanResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(planService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlanResponse> create(
            @Valid @RequestBody CreatePlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlanResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody CreatePlanRequest request) {
        return ResponseEntity.ok(planService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Integer id) {
        planService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
