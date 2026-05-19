package com.gymiq.controller;

import com.gymiq.dto.request.CreateStudentRequest;
import com.gymiq.dto.request.UpdateStudentRequest;
import com.gymiq.dto.response.AddressLookupResponse;
import com.gymiq.dto.response.StudentOptionResponse;
import com.gymiq.dto.response.StudentResponse;
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
    public ResponseEntity<Page<StudentResponse>> findAll(
            @PageableDefault(size = 10, sort = "user.name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(studentService.findAll(pageable));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION','INSTRUCTOR')")
    public ResponseEntity<Page<StudentResponse>> search(
            @RequestParam String q,
            @PageableDefault(size = 10, sort = "user.name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(studentService.search(q, pageable));
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

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION','INSTRUCTOR','STUDENT')")
    public ResponseEntity<StudentResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(studentService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<StudentResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateStudentRequest request) {
        return ResponseEntity.ok(studentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Integer id) {
        studentService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
