package com.gymiq.dto.response;

import com.gymiq.entity.WorkoutBlock;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class WorkoutBlockSummaryResponse {

    private UUID workoutBlockId;
    private UUID workoutSheetId;
    private String name;
    private String description;
    private Integer executionOrder;
    private Boolean active;
    private Integer exerciseCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static WorkoutBlockSummaryResponse fromEntity(WorkoutBlock block) {
        return WorkoutBlockSummaryResponse.builder()
                .workoutBlockId(block.getWorkoutBlockId())
                .workoutSheetId(block.getWorkoutSheet().getWorkoutSheetId())
                .name(block.getName())
                .description(block.getDescription())
                .executionOrder(block.getExecutionOrder())
                .active(block.getActive())
                .exerciseCount(block.getExercises().size())
                .createdAt(block.getCreatedAt())
                .updatedAt(block.getUpdatedAt())
                .build();
    }
}
