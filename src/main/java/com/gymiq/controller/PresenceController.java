package com.gymiq.controller;

import com.gymiq.dto.request.CheckOutPresenceRequest;
import com.gymiq.dto.request.CreatePresenceRequest;
import com.gymiq.dto.response.PresenceResponse;
import com.gymiq.service.PresenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/presences")
@RequiredArgsConstructor
public class PresenceController {

    private final PresenceService presenceService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION','STUDENT')")
    public ResponseEntity<PresenceResponse> checkIn(
            @Valid @RequestBody CreatePresenceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(presenceService.checkIn(request));
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
    public ResponseEntity<List<PresenceResponse>> findAll() {
        return ResponseEntity.ok(presenceService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<PresenceResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(presenceService.findById(id));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION','STUDENT')")
    public ResponseEntity<List<PresenceResponse>> findByStudent(@PathVariable Integer studentId) {
        return ResponseEntity.ok(presenceService.findByStudent(studentId));
    }

    @GetMapping("/date/{date}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<List<PresenceResponse>> findByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(presenceService.findByDate(date));
    }
}
