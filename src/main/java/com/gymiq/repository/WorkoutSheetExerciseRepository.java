package com.gymiq.repository;

import com.gymiq.entity.WorkoutSheetExercise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkoutSheetExerciseRepository extends JpaRepository<WorkoutSheetExercise, Integer> {

    List<WorkoutSheetExercise> findByWorkoutSheetWorkoutSheetIdOrderByExecutionOrderAsc(Integer workoutSheetId);

    Page<WorkoutSheetExercise> findByWorkoutSheetWorkoutSheetId(Integer workoutSheetId, Pageable pageable);
}
