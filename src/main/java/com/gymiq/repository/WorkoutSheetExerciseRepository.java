package com.gymiq.repository;

import com.gymiq.entity.WorkoutSheetExercise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkoutSheetExerciseRepository extends JpaRepository<WorkoutSheetExercise, UUID> {

    List<WorkoutSheetExercise> findByWorkoutBlockWorkoutSheetWorkoutSheetIdOrderByWorkoutBlockExecutionOrderAscExecutionOrderAsc(UUID workoutSheetId);

    Page<WorkoutSheetExercise> findByWorkoutBlockWorkoutSheetWorkoutSheetId(UUID workoutSheetId, Pageable pageable);

    Page<WorkoutSheetExercise> findByWorkoutBlockWorkoutBlockId(UUID workoutBlockId, Pageable pageable);

    boolean existsByExerciseExerciseId(Integer exerciseId);
}
