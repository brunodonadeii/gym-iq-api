package com.gymiq.controller;

import com.gymiq.dto.request.CheckOutPresenceRequest;
import com.gymiq.dto.request.CreatePresenceRequest;
import com.gymiq.dto.request.SelfCheckInRequest;
import com.gymiq.dto.response.PresenceResponse;
import com.gymiq.service.PresenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/presences")
@RequiredArgsConstructor
public class PresenceController {

    private final PresenceService presenceService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<PresenceResponse> checkIn(
            @Valid @RequestBody CreatePresenceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(presenceService.checkIn(request));
    }

    @PostMapping("/self-check-in")
    public ResponseEntity<PresenceResponse> selfCheckIn(
            @Valid @RequestBody SelfCheckInRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(presenceService.selfCheckIn(request));
    }

    @PatchMapping("/{id}/checkout")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<PresenceResponse> checkOut(
            @PathVariable Integer id,
            @Valid @RequestBody(required = false) CheckOutPresenceRequest request) {
        return ResponseEntity.ok(presenceService.checkOut(id, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<Page<PresenceResponse>> findAll(
            @PageableDefault(size = 10, sort = "checkInAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(presenceService.findAll(pageable));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Page<PresenceResponse>> findMine(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "checkInAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(presenceService.findByAuthenticatedStudent(authentication.getName(), pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<PresenceResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(presenceService.findById(id));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<Page<PresenceResponse>> findByStudent(
            @PathVariable Integer studentId,
            @PageableDefault(size = 10, sort = "checkInAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(presenceService.findByStudent(studentId, pageable));
    }

    @GetMapping("/date/{date}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<Page<PresenceResponse>> findByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PageableDefault(size = 10, sort = "checkInAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(presenceService.findByDate(date, pageable));
    }
}
