package com.gymiq.repository;

import com.gymiq.entity.WorkoutSheet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkoutSheetRepository extends JpaRepository<WorkoutSheet, Integer> {

    Page<WorkoutSheet> findByStudentStudentId(Integer studentId, Pageable pageable);

    Page<WorkoutSheet> findByStudentStudentIdAndActiveTrue(Integer studentId, Pageable pageable);

    Page<WorkoutSheet> findByStudentStudentIdAndInstructorUserEmailHash(
            Integer studentId,
            String emailHash,
            Pageable pageable);

    Page<WorkoutSheet> findByStudentStudentIdAndInstructorUserEmailHashAndActiveTrue(
            Integer studentId,
            String emailHash,
            Pageable pageable);

    Page<WorkoutSheet> findByInstructorInstructorId(Integer instructorId, Pageable pageable);

    boolean existsByInstructorInstructorId(Integer instructorId);
}
