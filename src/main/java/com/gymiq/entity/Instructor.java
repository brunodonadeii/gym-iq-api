package com.gymiq.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "instructor",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_instructor_cref", columnNames = "cref")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Instructor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_instructor")
    private Integer instructorId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_instructor_user"))
    private User user;

    @Column(name = "cref", nullable = false, length = 20)
    private String cref;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "specialty", length = 100)
    private String specialty;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "instructor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WorkoutSheet> workoutSheets;
}
