package com.gymiq.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "exercise",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_exercise_name", columnNames = "name")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_exercise")
    private Integer exerciseId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "muscle_group", nullable = false, length = 80)
    private String muscleGroup;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "exercise", fetch = FetchType.LAZY)
    private List<WorkoutSheetExercise> workoutSheetExercises;
}
