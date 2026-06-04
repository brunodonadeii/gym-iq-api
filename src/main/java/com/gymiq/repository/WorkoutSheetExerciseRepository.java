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

    List<WorkoutSheetExercise> findByWorkoutSheetWorkoutSheetIdOrderByTrainingSectionAscExecutionOrderAsc(UUID workoutSheetId);

    Page<WorkoutSheetExercise> findByWorkoutSheetWorkoutSheetId(UUID workoutSheetId, Pageable pageable);

    boolean existsByExerciseExerciseId(Integer exerciseId);
}
