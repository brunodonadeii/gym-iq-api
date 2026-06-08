package com.gymiq.dto.response;

import com.gymiq.entity.WorkoutSheet;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class WorkoutSheetSummaryResponse {

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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static WorkoutSheetSummaryResponse fromEntity(WorkoutSheet workoutSheet) {
        return WorkoutSheetSummaryResponse.builder()
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
                .createdAt(workoutSheet.getCreatedAt())
                .updatedAt(workoutSheet.getUpdatedAt())
                .build();
    }
}
