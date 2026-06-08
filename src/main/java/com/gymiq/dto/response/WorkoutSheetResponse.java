package com.gymiq.dto.response;

import com.gymiq.entity.WorkoutSheet;
import com.gymiq.entity.WorkoutSheetExercise;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class WorkoutSheetResponse {

    private UUID workoutSheetId;
    private UUID studentId;
    private String studentName;
    private UUID instructorId;
    private String instructorName;
    private String name;
    private String goal;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean active;
    private String notes;
    private List<WorkoutBlockResponse> blocks;
    private List<WorkoutSheetExerciseResponse> exercises;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static WorkoutSheetResponse fromEntity(WorkoutSheet workoutSheet) {
        return WorkoutSheetResponse.builder()
                .workoutSheetId(workoutSheet.getWorkoutSheetId())
                .studentId(workoutSheet.getStudent().getStudentId())
                .studentName(workoutSheet.getStudent().getUser().getName())
                .instructorId(workoutSheet.getInstructor().getInstructorId())
                .instructorName(workoutSheet.getInstructor().getUser().getName())
                .name(workoutSheet.getName())
                .goal(workoutSheet.getGoal())
                .startDate(workoutSheet.getStartDate())
                .endDate(workoutSheet.getEndDate())
                .active(workoutSheet.getActive())
                .notes(workoutSheet.getNotes())
                .blocks(mapBlocks(workoutSheet))
                .exercises(mapExercises(workoutSheet))
                .createdAt(workoutSheet.getCreatedAt())
                .updatedAt(workoutSheet.getUpdatedAt())
                .build();
    }

    private static List<WorkoutBlockResponse> mapBlocks(WorkoutSheet workoutSheet) {
        return workoutSheet.getBlocks()
                .stream()
                .sorted(Comparator.comparing(block -> block.getExecutionOrder()))
                .map(WorkoutBlockResponse::fromEntity)
                .toList();
    }

    private static List<WorkoutSheetExerciseResponse> mapExercises(WorkoutSheet workoutSheet) {
        return workoutSheet.getBlocks()
                .stream()
                .flatMap(block -> block.getExercises().stream())
                .sorted(Comparator
                        .<WorkoutSheetExercise, Integer>comparing(item -> item.getWorkoutBlock().getExecutionOrder())
                        .thenComparing(WorkoutSheetExercise::getExecutionOrder))
                .map(WorkoutSheetExerciseResponse::fromEntity)
                .toList();
    }
}
