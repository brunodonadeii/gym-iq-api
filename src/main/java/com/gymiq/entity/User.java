package com.gymiq.entity;

import com.gymiq.entity.converter.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_email_hash", columnNames = "email_hash")
        },
        indexes = {
                @Index(name = "idx_user_name", columnList = "name"),
                @Index(name = "idx_user_active", columnList = "active"),
                @Index(name = "idx_user_email_hash", columnList = "email_hash")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_user", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "email", nullable = false, columnDefinition = "TEXT")
    private String email;

    @Column(name = "email_hash", nullable = false, length = 64)
    private String emailHash;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "lgpd_accepted", nullable = false)
    @Builder.Default
    private Boolean lgpdAccepted = false;

    @Column(name = "lgpd_accepted_at")
    private LocalDateTime lgpdAcceptedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Student student;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Instructor instructor;

    public enum Role {
        ADMIN,
        RECEPTION,
        INSTRUCTOR,
        STUDENT
    }
}
