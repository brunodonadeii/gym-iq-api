package com.gymiq.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "workout_block",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_workout_block_name",
                        columnNames = {"workout_sheet_id", "name"}),
                @UniqueConstraint(name = "uk_workout_block_order",
                        columnNames = {"workout_sheet_id", "execution_order"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_workout_block")
    private UUID workoutBlockId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_sheet_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_workout_block_sheet"))
    private WorkoutSheet workoutSheet;

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "execution_order", nullable = false)
    private Integer executionOrder;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "workoutBlock", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<WorkoutSheetExercise> exercises = new ArrayList<>();
}
