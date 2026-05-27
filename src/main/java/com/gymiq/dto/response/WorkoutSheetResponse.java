package com.gymiq.dto.response;

import com.gymiq.entity.WorkoutSheet;
import com.gymiq.entity.WorkoutSheetExercise;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Data
@Builder
public class WorkoutSheetResponse {

    private Integer workoutSheetId;
    private Integer studentId;
    private String studentName;
    private Integer instructorId;
    private String instructorName;
    private String name;
    private String goal;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean active;
    private String notes;
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
                .exercises(mapExercises(workoutSheet))
                .createdAt(workoutSheet.getCreatedAt())
                .updatedAt(workoutSheet.getUpdatedAt())
                .build();
    }

    private static List<WorkoutSheetExerciseResponse> mapExercises(WorkoutSheet workoutSheet) {
        return workoutSheet.getExercises()
                .stream()
                .sorted(Comparator.comparing(WorkoutSheetExercise::getExecutionOrder))
                .map(WorkoutSheetExerciseResponse::fromEntity)
                .toList();
    }
}
