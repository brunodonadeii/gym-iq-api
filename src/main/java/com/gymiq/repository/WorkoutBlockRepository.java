package com.gymiq.repository;

import com.gymiq.entity.WorkoutBlock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkoutBlockRepository extends JpaRepository<WorkoutBlock, UUID> {

    List<WorkoutBlock> findByWorkoutSheetWorkoutSheetIdOrderByExecutionOrderAsc(UUID workoutSheetId);

    Page<WorkoutBlock> findByWorkoutSheetWorkoutSheetId(UUID workoutSheetId, Pageable pageable);

    Optional<WorkoutBlock> findByWorkoutSheetWorkoutSheetIdAndNameIgnoreCase(UUID workoutSheetId, String name);

    boolean existsByWorkoutSheetWorkoutSheetIdAndExecutionOrder(UUID workoutSheetId, Integer executionOrder);

    boolean existsByWorkoutSheetWorkoutSheetIdAndNameIgnoreCase(UUID workoutSheetId, String name);
}
