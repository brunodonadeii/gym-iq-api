package com.gymiq.repository;

import com.gymiq.entity.WorkoutSheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkoutSheetRepository extends JpaRepository<WorkoutSheet, Integer> {

    List<WorkoutSheet> findByStudentStudentIdOrderByCreatedAtDesc(Integer studentId);

    List<WorkoutSheet> findByStudentStudentIdAndActiveTrueOrderByCreatedAtDesc(Integer studentId);

    List<WorkoutSheet> findByInstructorInstructorIdOrderByCreatedAtDesc(Integer instructorId);
}
