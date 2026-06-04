package com.gymiq.dto.response;

import com.gymiq.entity.WorkoutSheetExercise;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class WorkoutSheetExerciseResponse {

    private UUID workoutSheetExerciseId;
    private Integer exerciseId;
    private String exerciseName;
    private String muscleGroup;
    private Integer sets;
    private String repetitions;
    private Integer restSeconds;
    private String trainingSection;
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
                .restSeconds(item.getRestSeconds())
                .trainingSection(item.getTrainingSection())
                .executionOrder(item.getExecutionOrder())
                .notes(item.getNotes())
                .build();
    }
}
