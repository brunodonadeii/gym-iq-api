package com.gymiq.repository;

import java.util.UUID;

import com.gymiq.entity.WorkoutSheet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkoutSheetRepository extends JpaRepository<WorkoutSheet, UUID> {

    Page<WorkoutSheet> findByStudentStudentId(UUID studentId, Pageable pageable);

    Page<WorkoutSheet> findByStudentStudentIdAndActiveTrue(UUID studentId, Pageable pageable);

    Page<WorkoutSheet> findByStudentStudentIdAndInstructorUserEmailHash(
            UUID studentId,
            String emailHash,
            Pageable pageable);

    Page<WorkoutSheet> findByStudentStudentIdAndInstructorUserEmailHashAndActiveTrue(
            UUID studentId,
            String emailHash,
            Pageable pageable);

    Page<WorkoutSheet> findByInstructorInstructorId(UUID instructorId, Pageable pageable);

    boolean existsByInstructorInstructorId(UUID instructorId);
}
