package com.gymiq.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "workout_sheet_exercise",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_workout_sheet_exercise_order",
                        columnNames = {"workout_sheet_id", "execution_order"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutSheetExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_workout_sheet_exercise")
    private Integer workoutSheetExerciseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_sheet_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_workout_sheet_exercise_sheet"))
    private WorkoutSheet workoutSheet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_workout_sheet_exercise_exercise"))
    private Exercise exercise;

    @Column(name = "sets", nullable = false)
    private Integer sets;

    @Column(name = "repetitions", nullable = false, length = 50)
    private String repetitions;

    @Column(name = "rest_seconds")
    private Integer restSeconds;

    @Column(name = "execution_order", nullable = false)
    private Integer executionOrder;

    @Column(name = "notes", length = 255)
    private String notes;
}
