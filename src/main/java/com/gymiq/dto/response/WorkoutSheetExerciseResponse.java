package com.gymiq.dto.response;

import com.gymiq.entity.WorkoutSheetExercise;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class WorkoutSheetExerciseResponse {

    private Integer workoutSheetExerciseId;
    private Integer exerciseId;
    private String exerciseName;
    private String muscleGroup;
    private Integer sets;
    private String repetitions;
    private BigDecimal loadKg;
    private Integer restSeconds;
    private Integer executionOrder;
    private String notes;

    public static WorkoutSheetExerciseResponse fromEntity(WorkoutSheetExercise item) {
        return WorkoutSheetExerciseResponse.builder()
                .workoutSheetExerciseId(item.getWorkoutSheetExerciseId())
                .exerciseId(item.getExercise().getExerciseId())
                .exerciseName(item.getExercise().getName())
                .muscleGroup(item.getExercise().getMuscleGroup())
                .sets(item.getSets())
                .repetitions(item.getRepetitions())
                .loadKg(item.getLoadKg())
                .restSeconds(item.getRestSeconds())
                .executionOrder(item.getExecutionOrder())
                .notes(item.getNotes())
                .build();
    }
}
