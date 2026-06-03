package com.gymiq.entity;

import com.gymiq.entity.converter.EncryptedLocalDateConverter;
import com.gymiq.entity.converter.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "student",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_student_cpf_hash", columnNames = "cpf_hash")
        },
        indexes = {
                @Index(name = "idx_student_user_id", columnList = "user_id"),
                @Index(name = "idx_student_cpf_hash", columnList = "cpf_hash")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_student")
    private Integer studentId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_student_user"))
    private User user;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "cpf", nullable = false, columnDefinition = "TEXT")
    private String cpf;

    @Column(name = "cpf_hash", nullable = false, length = 64)
    private String cpfHash;

    @Convert(converter = EncryptedLocalDateConverter.class)
    @Column(name = "birth_date", nullable = false, columnDefinition = "TEXT")
    private LocalDate birthDate;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "phone", nullable = false, columnDefinition = "TEXT")
    private String phone;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "zip_code", columnDefinition = "TEXT")
    private String zipCode;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Enrollment> enrollments;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Presence> presences;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WorkoutSheet> workoutSheets;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RetentionAlert> retentionAlerts;
}
