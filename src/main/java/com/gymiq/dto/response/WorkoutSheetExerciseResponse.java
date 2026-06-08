package com.gymiq.dto.response;

import com.gymiq.entity.WorkoutSheetExercise;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class WorkoutSheetExerciseResponse {

    private UUID workoutSheetExerciseId;
    private UUID workoutBlockId;
    private String blockName;
    private Integer exerciseId;
    private String exerciseName;
    private String muscleGroup;
    private Integer sets;
    private String repetitions;
    private Integer restSeconds;
    private Integer executionOrder;
    private String notes;

    public static WorkoutSheetExerciseResponse fromEntity(WorkoutSheetExercise item) {
        return WorkoutSheetExerciseResponse.builder()
                .workoutSheetExerciseId(item.getWorkoutSheetExerciseId())
                .workoutBlockId(item.getWorkoutBlock().getWorkoutBlockId())
                .blockName(item.getWorkoutBlock().getName())
                .exerciseId(item.getExercise().getExerciseId())
                .exerciseName(item.getExercise().getName())
                .muscleGroup(item.getExercise().getMuscleGroup())
                .sets(item.getSets())
                .repetitions(item.getRepetitions())
                .restSeconds(item.getRestSeconds())
                .executionOrder(item.getExecutionOrder())
                .notes(item.getNotes())
                .build();
    }
}
