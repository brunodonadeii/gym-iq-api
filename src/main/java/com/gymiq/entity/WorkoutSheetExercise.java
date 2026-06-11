package com.gymiq.entity;

import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "workout_sheet_exercise",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_workout_sheet_exercise_order",
                        columnNames = {"workout_block_id", "execution_order"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutSheetExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_workout_sheet_exercise")
    private UUID workoutSheetExerciseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_block_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_workout_sheet_exercise_block"))
    private WorkoutBlock workoutBlock;

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
