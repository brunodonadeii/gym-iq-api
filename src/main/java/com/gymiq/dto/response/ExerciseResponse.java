package com.gymiq.dto.response;

import com.gymiq.entity.Exercise;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ExerciseResponse {

    private Integer exerciseId;
    private String name;
    private String muscleGroup;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ExerciseResponse fromEntity(Exercise exercise) {
        return ExerciseResponse.builder()
                .exerciseId(exercise.getExerciseId())
                .name(exercise.getName())
                .muscleGroup(exercise.getMuscleGroup())
                .description(exercise.getDescription())
                .createdAt(exercise.getCreatedAt())
                .updatedAt(exercise.getUpdatedAt())
                .build();
    }
}
