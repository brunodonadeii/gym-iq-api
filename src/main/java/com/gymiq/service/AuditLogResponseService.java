package com.gymiq.service;

import com.gymiq.dto.response.AuditLogResponse;
import com.gymiq.entity.AuditLog;
import com.gymiq.entity.Enrollment;
import com.gymiq.entity.Exercise;
import com.gymiq.entity.Instructor;
import com.gymiq.entity.Payment;
import com.gymiq.entity.Plan;
import com.gymiq.entity.Presence;
import com.gymiq.entity.RetentionAlert;
import com.gymiq.entity.Student;
import com.gymiq.entity.User;
import com.gymiq.entity.WorkoutBlock;
import com.gymiq.entity.WorkoutSheet;
import com.gymiq.entity.WorkoutSheetExercise;
import com.gymiq.enums.AuditAction;
import com.gymiq.enums.ResourceType;
import com.gymiq.repository.EnrollmentRepository;
import com.gymiq.repository.ExerciseRepository;
import com.gymiq.repository.InstructorRepository;
import com.gymiq.repository.PaymentRepository;
import com.gymiq.repository.PlanRepository;
import com.gymiq.repository.PresenceRepository;
import com.gymiq.repository.RetentionAlertRepository;
import com.gymiq.repository.StudentRepository;
import com.gymiq.repository.UserRepository;
import com.gymiq.repository.WorkoutBlockRepository;
import com.gymiq.repository.WorkoutSheetExerciseRepository;
import com.gymiq.repository.WorkoutSheetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogResponseService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final InstructorRepository instructorRepository;
    private final PlanRepository planRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PaymentRepository paymentRepository;
    private final PresenceRepository presenceRepository;
    private final ExerciseRepository exerciseRepository;
    private final WorkoutSheetRepository workoutSheetRepository;
    private final WorkoutBlockRepository workoutBlockRepository;
    private final WorkoutSheetExerciseRepository workoutSheetExerciseRepository;
    private final RetentionAlertRepository retentionAlertRepository;

    @Transactional(readOnly = true)
    public AuditLogResponse toResponse(AuditLog log) {
        return toResponse(log, new ResolutionContext());
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> toResponsePage(Page<AuditLog> logs) {
        ResolutionContext context = new ResolutionContext();
        List<AuditLogResponse> responses = logs.getContent().stream()
                .map(log -> toResponse(log, context))
                .toList();
        return new PageImpl<>(responses, logs.getPageable(), logs.getTotalElements());
    }

    private AuditLogResponse toResponse(AuditLog log, ResolutionContext context) {
        AuditAction action = log.getAction();
        ResourceType resourceType = log.getResourceType();
        Optional<User> actorUser = resolveActorUser(log, context);

        return AuditLogResponse.builder()
                .auditLogId(log.getAuditLogId())
                .actorUserId(log.getActorUserId())
                .actorEmail(resolveActorEmail(log, actorUser))
                .actorLabel(resolveActorLabel(log, actorUser))
                .actorRole(log.getActorRole())
                .action(action)
                .actionLabel(action == null ? null : action.getLabel())
                .resourceType(resourceType)
                .resourceId(log.getResourceId())
                .resourceLabel(resolveResourceLabel(resourceType, log.getResourceId(), context))
                .description(log.getDescription())
                .ipAddress(log.getIpAddress())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private Optional<User> resolveActorUser(AuditLog log, ResolutionContext context) {
        if (log.getActorUserId() == null) {
            return Optional.empty();
        }
        return context.userCache.computeIfAbsent(log.getActorUserId(), userRepository::findById);
    }

    private String resolveActorLabel(AuditLog log, Optional<User> actorUser) {
        return actorUser
                .map(this::formatUserLabel)
                .orElseGet(() -> fallbackActorLabel(log));
    }

    private String resolveActorEmail(AuditLog log, Optional<User> actorUser) {
        if (actorUser.isPresent()) {
            return actorUser
                    .filter(user -> isAdministrativeRole(user.getRole()))
                    .map(User::getEmail)
                    .orElse(null);
        }

        if (isAdministrativeRole(log.getActorRole())) {
            return log.getActorEmail();
        }

        return null;
    }

    private String fallbackActorLabel(AuditLog log) {
        if (log.getActorEmail() != null && !log.getActorEmail().isBlank()) {
            if (!isAdministrativeRole(log.getActorRole())) {
                return log.getActorRole() == null ? "Usuário" : log.getActorRole();
            }
            return log.getActorEmail();
        }
        return "Sistema";
    }

    private String resolveResourceLabel(ResourceType resourceType, String resourceId, ResolutionContext context) {
        if (resourceType == null) {
            return null;
        }
        if (resourceType == ResourceType.JOB) {
            return "Rotina automática";
        }
        if (resourceId == null || resourceId.isBlank()) {
            return resourceType.getLabel();
        }

        String cacheKey = resourceType.name() + ":" + resourceId;
        return context.resourceLabelCache.computeIfAbsent(cacheKey, ignored -> switch (resourceType) {
            case USER -> findUuid(resourceId)
                    .flatMap(id -> context.userCache.computeIfAbsent(id, userRepository::findById))
                    .map(this::formatUserLabel)
                    .orElse(defaultLabel(resourceType, resourceId));
            case STUDENT -> findUuid(resourceId)
                    .flatMap(id -> context.studentCache.computeIfAbsent(id, studentRepository::findById))
                    .map(student -> student.getUser().getName())
                    .orElse(defaultLabel(resourceType, resourceId));
            case INSTRUCTOR -> findUuid(resourceId)
                    .flatMap(id -> context.instructorCache.computeIfAbsent(id, instructorRepository::findById))
                    .map(instructor -> instructor.getUser().getName())
                    .orElse(defaultLabel(resourceType, resourceId));
            case PLAN -> findInteger(resourceId)
                    .flatMap(id -> context.planCache.computeIfAbsent(id, planRepository::findById))
                    .map(Plan::getName)
                    .orElse(defaultLabel(resourceType, resourceId));
            case ENROLLMENT -> findUuid(resourceId)
                    .flatMap(id -> context.enrollmentCache.computeIfAbsent(id, enrollmentRepository::findById))
                    .map(enrollment -> "Matrícula de " + enrollment.getStudent().getUser().getName())
                    .orElse(defaultLabel(resourceType, resourceId));
            case PAYMENT -> findUuid(resourceId)
                    .flatMap(id -> context.paymentCache.computeIfAbsent(id, paymentRepository::findById))
                    .map(payment -> "Pagamento de " + payment.getEnrollment().getStudent().getUser().getName())
                    .orElse(defaultLabel(resourceType, resourceId));
            case PRESENCE -> findUuid(resourceId)
                    .flatMap(id -> context.presenceCache.computeIfAbsent(id, presenceRepository::findById))
                    .map(presence -> "Check-in de " + presence.getStudent().getUser().getName())
                    .orElse(defaultLabel(resourceType, resourceId));
            case EXERCISE -> findInteger(resourceId)
                    .flatMap(id -> context.exerciseCache.computeIfAbsent(id, exerciseRepository::findById))
                    .map(Exercise::getName)
                    .orElse(defaultLabel(resourceType, resourceId));
            case WORKOUT_SHEET -> findUuid(resourceId)
                    .flatMap(id -> context.workoutSheetCache.computeIfAbsent(id, workoutSheetRepository::findById))
                    .map(WorkoutSheet::getName)
                    .orElse(defaultLabel(resourceType, resourceId));
            case WORKOUT_BLOCK -> findUuid(resourceId)
                    .flatMap(id -> context.workoutBlockCache.computeIfAbsent(id, workoutBlockRepository::findById))
                    .map(this::formatWorkoutBlockLabel)
                    .orElse(defaultLabel(resourceType, resourceId));
            case WORKOUT_SHEET_EXERCISE -> findUuid(resourceId)
                    .flatMap(id -> context.workoutSheetExerciseCache.computeIfAbsent(id, workoutSheetExerciseRepository::findById))
                    .map(this::formatWorkoutSheetExerciseLabel)
                    .orElse(defaultLabel(resourceType, resourceId));
            case RETENTION_ALERT -> findUuid(resourceId)
                    .flatMap(id -> context.retentionAlertCache.computeIfAbsent(id, retentionAlertRepository::findById))
                    .map(alert -> "Alerta de " + alert.getStudent().getUser().getName())
                    .orElse(defaultLabel(resourceType, resourceId));
            case JOB -> "Rotina automática";
        });
    }

    private String formatUserLabel(User user) {
        if (isAdministrativeRole(user.getRole())) {
            return user.getName() + " (" + user.getEmail() + ")";
        }
        return user.getName();
    }

    private boolean isAdministrativeRole(User.Role role) {
        return role == User.Role.ADMIN || role == User.Role.RECEPTION;
    }

    private boolean isAdministrativeRole(String role) {
        return User.Role.ADMIN.name().equals(role) || User.Role.RECEPTION.name().equals(role);
    }

    private String formatWorkoutBlockLabel(WorkoutBlock workoutBlock) {
        return workoutBlock.getWorkoutSheet().getName() + " - " + workoutBlock.getName();
    }

    private String formatWorkoutSheetExerciseLabel(WorkoutSheetExercise workoutSheetExercise) {
        return workoutSheetExercise.getWorkoutBlock().getWorkoutSheet().getName()
                + " - "
                + workoutSheetExercise.getExercise().getName();
    }

    private Optional<UUID> findUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private Optional<Integer> findInteger(String value) {
        try {
            return Optional.of(Integer.valueOf(value));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private String defaultLabel(ResourceType resourceType, String resourceId) {
        return resourceType.getLabel() + " #" + resourceId;
    }

    private static class ResolutionContext {
        private final Map<UUID, Optional<User>> userCache = new HashMap<>();
        private final Map<UUID, Optional<Student>> studentCache = new HashMap<>();
        private final Map<UUID, Optional<Instructor>> instructorCache = new HashMap<>();
        private final Map<Integer, Optional<Plan>> planCache = new HashMap<>();
        private final Map<UUID, Optional<Enrollment>> enrollmentCache = new HashMap<>();
        private final Map<UUID, Optional<Payment>> paymentCache = new HashMap<>();
        private final Map<UUID, Optional<Presence>> presenceCache = new HashMap<>();
        private final Map<Integer, Optional<Exercise>> exerciseCache = new HashMap<>();
        private final Map<UUID, Optional<WorkoutSheet>> workoutSheetCache = new HashMap<>();
        private final Map<UUID, Optional<WorkoutBlock>> workoutBlockCache = new HashMap<>();
        private final Map<UUID, Optional<WorkoutSheetExercise>> workoutSheetExerciseCache = new HashMap<>();
        private final Map<UUID, Optional<RetentionAlert>> retentionAlertCache = new HashMap<>();
        private final Map<String, String> resourceLabelCache = new HashMap<>();
    }
}
