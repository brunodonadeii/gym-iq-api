package com.gymiq.dto.response;

import com.gymiq.entity.WorkoutBlock;
import com.gymiq.entity.WorkoutSheetExercise;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class WorkoutBlockResponse {

    private UUID workoutBlockId;
    private UUID workoutSheetId;
    private String name;
    private String description;
    private Integer executionOrder;
    private Boolean active;
    private List<WorkoutSheetExerciseResponse> exercises;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static WorkoutBlockResponse fromEntity(WorkoutBlock block) {
        return WorkoutBlockResponse.builder()
                .workoutBlockId(block.getWorkoutBlockId())
                .workoutSheetId(block.getWorkoutSheet().getWorkoutSheetId())
                .name(block.getName())
                .description(block.getDescription())
                .executionOrder(block.getExecutionOrder())
                .active(block.getActive())
                .exercises(mapExercises(block))
                .createdAt(block.getCreatedAt())
                .updatedAt(block.getUpdatedAt())
                .build();
    }

    private static List<WorkoutSheetExerciseResponse> mapExercises(WorkoutBlock block) {
        return block.getExercises()
                .stream()
                .sorted(Comparator.comparing(WorkoutSheetExercise::getExecutionOrder))
                .map(WorkoutSheetExerciseResponse::fromEntity)
                .toList();
    }
}
