package com.gymiq.controller;

import java.util.UUID;

import com.gymiq.dto.request.CreateStudentRequest;
import com.gymiq.dto.request.StudentStatusFilter;
import com.gymiq.dto.request.UpdateStudentRequest;
import com.gymiq.dto.response.AddressLookupResponse;
import com.gymiq.dto.response.StudentDataDeletionEligibilityResponse;
import com.gymiq.dto.response.StudentOptionResponse;
import com.gymiq.dto.response.StudentResponse;
import com.gymiq.dto.response.StudentSummaryResponse;
import com.gymiq.service.AddressLookupService;
import com.gymiq.service.StudentService;
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

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final AddressLookupService addressLookupService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<StudentResponse> create(
            @Valid @RequestBody CreateStudentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION','INSTRUCTOR')")
    public ResponseEntity<Page<StudentSummaryResponse>> findAll(
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "ACTIVE") StudentStatusFilter status,
            @PageableDefault(size = 10, sort = "user.name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(studentService.findAll(status, hasRole(authentication, "ADMIN"), pageable));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION','INSTRUCTOR')")
    public ResponseEntity<Page<StudentSummaryResponse>> search(
            Authentication authentication,
            @RequestParam String q,
            @RequestParam(required = false, defaultValue = "ACTIVE") StudentStatusFilter status,
            @PageableDefault(size = 10, sort = "user.name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(studentService.search(q, status, hasRole(authentication, "ADMIN"), pageable));
    }

    @GetMapping("/options")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION','INSTRUCTOR')")
    public ResponseEntity<List<StudentOptionResponse>> findOptions(
            @RequestParam(required = false, defaultValue = "") String q) {
        return ResponseEntity.ok(studentService.findOptions(q));
    }

    @GetMapping("/address-by-zip-code/{zipCode}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<AddressLookupResponse> findAddressByZipCode(@PathVariable String zipCode) {
        return ResponseEntity.ok(addressLookupService.lookupByZipCode(zipCode));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentResponse> findMe(Authentication authentication) {
        return ResponseEntity.ok(studentService.findByAuthenticatedEmail(authentication.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION','INSTRUCTOR')")
    public ResponseEntity<StudentResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(studentService.findById(id));
    }

    @GetMapping("/{id}/personal-data/deletion-eligibility")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<StudentDataDeletionEligibilityResponse> checkDataDeletionEligibility(@PathVariable UUID id) {
        return ResponseEntity.ok(studentService.checkDataDeletionEligibility(id));
    }

    @GetMapping("/me/personal-data/deletion-eligibility")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentDataDeletionEligibilityResponse> checkMyDataDeletionEligibility(Authentication authentication) {
        return ResponseEntity.ok(studentService.checkAuthenticatedDataDeletionEligibility(authentication.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<StudentResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStudentRequest request) {
        return ResponseEntity.ok(studentService.update(id, request));
    }

    @PatchMapping("/{id}/inactive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        studentService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudentResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(studentService.activate(id));
    }

    @PatchMapping("/{id}/anonymize")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudentResponse> anonymize(@PathVariable UUID id) {
        return ResponseEntity.ok(studentService.anonymize(id));
    }

    @DeleteMapping("/{id}/personal-data")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<StudentResponse> anonymizePersonalData(@PathVariable UUID id) {
        return ResponseEntity.ok(studentService.anonymize(id));
    }

    @DeleteMapping("/me/personal-data")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentResponse> anonymizeMyPersonalData(Authentication authentication) {
        return ResponseEntity.ok(studentService.anonymizeAuthenticatedStudent(authentication.getName()));
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }
}
