package com.gymiq.dto.response;

import com.gymiq.entity.Enrollment;
import com.gymiq.entity.Payment;
import com.gymiq.entity.Presence;
import com.gymiq.entity.RetentionAlert;
import com.gymiq.entity.User;
import com.gymiq.entity.WorkoutSheet;
import com.gymiq.enums.AuditAction;
import com.gymiq.enums.ResourceType;
import com.gymiq.support.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseMappingTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void studentAndInstructorResponsesShouldMaskSensitiveDataWithoutAuthorizedRole() {
        StudentResponse student = StudentResponse.fromEntity(TestDataFactory.activeStudent());
        StudentSummaryResponse summary = StudentSummaryResponse.fromEntity(TestDataFactory.activeStudent());
        InstructorResponse instructor = InstructorResponse.fromEntity(TestDataFactory.activeInstructor());

        assertThat(student.getEmail()).contains("***");
        assertThat(student.getCpf()).contains("***");
        assertThat(student.getBirthDate()).isNull();
        assertThat(student.getAddress()).isNull();
        assertThat(summary.getEmail()).contains("***");
        assertThat(instructor.getEmail()).contains("***");
        assertThat(instructor.getPhone()).contains("***");
        assertThat(instructor.getSpecialty()).isEqualTo("Musculacao");
    }

    @Test
    void coreEntityResponsesShouldMapNestedFields() {
        Enrollment enrollment = TestDataFactory.activeEnrollment();
        Payment payment = TestDataFactory.pendingPayment();
        Presence presence = TestDataFactory.openPresence();
        RetentionAlert alert = TestDataFactory.openRetentionAlert();

        EnrollmentResponse enrollmentResponse = EnrollmentResponse.fromEntity(enrollment, List.of(payment));
        PaymentResponse paymentResponse = PaymentResponse.fromEntity(payment);
        PresenceResponse presenceResponse = PresenceResponse.fromEntity(presence);
        RetentionAlertResponse retentionResponse = RetentionAlertResponse.fromEntity(alert);

        assertThat(enrollmentResponse.getStudentName()).isEqualTo("Ana Silva");
        assertThat(enrollmentResponse.getPlanName()).isEqualTo("Mensal");
        assertThat(enrollmentResponse.getPayments()).hasSize(1);
        assertThat(paymentResponse.getStudentId()).isEqualTo(enrollment.getStudent().getStudentId());
        assertThat(paymentResponse.getPlanName()).isEqualTo("Mensal");
        assertThat(presenceResponse.getCheckInAt()).isEqualTo(presence.getCheckInAt());
        assertThat(retentionResponse.getRiskLevel()).isEqualTo("LOW");
        assertThat(retentionResponse.getStatus()).isEqualTo("OPEN");
    }

    @Test
    void workoutSheetResponsesShouldMapSummaryDetailAndExerciseItems() {
        WorkoutSheet workoutSheet = TestDataFactory.workoutSheet();

        WorkoutSheetResponse detail = WorkoutSheetResponse.fromEntity(workoutSheet);
        WorkoutSheetSummaryResponse summary = WorkoutSheetSummaryResponse.fromEntity(workoutSheet);
        WorkoutSheetExerciseResponse item = WorkoutSheetExerciseResponse.fromEntity(
                workoutSheet.getBlocks().get(0).getExercises().get(0));

        assertThat(detail.getWorkoutSheetId()).isEqualTo(workoutSheet.getWorkoutSheetId());
        assertThat(detail.getStudentName()).isEqualTo("Ana Silva");
        assertThat(detail.getInstructorName()).isEqualTo("Carlos Trainer");
        assertThat(detail.getBlocks()).hasSize(1);
        assertThat(detail.getExercises()).hasSize(1);
        assertThat(summary.getName()).isEqualTo("Ficha Hipertrofia");
        assertThat(summary.getGoal()).isEqualTo("Ganho de massa");
        assertThat(summary.getBlockCount()).isEqualTo(1);
        assertThat(summary.getExerciseCount()).isEqualTo(1);
        assertThat(item.getExerciseName()).isEqualTo("Supino");
        assertThat(item.getBlockName()).isEqualTo("Treino A");
    }

    @Test
    void administrativeResponsesShouldMapValues() {
        User user = TestDataFactory.activeAdminUser();

        UserResponse userResponse = UserResponse.fromEntity(user);
        AuditLogResponse auditLogResponse = AuditLogResponse.builder()
                .auditLogId(1L)
                .actorUserId(user.getUserId())
                .actorEmail(user.getEmail())
                .actorLabel(user.getName() + " (" + user.getEmail() + ")")
                .actorRole(user.getRole().name())
                .action(AuditAction.LOGIN)
                .actionLabel(AuditAction.LOGIN.getLabel())
                .resourceType(ResourceType.USER)
                .resourceId(user.getUserId().toString())
                .resourceLabel(user.getName() + " (" + user.getEmail() + ")")
                .description("Realizou login")
                .ipAddress("127.0.0.1")
                .build();
        AuditFilterOptionResponse option = new AuditFilterOptionResponse(AuditAction.LOGIN.name(), AuditAction.LOGIN.getLabel());
        AuditActorOptionResponse actor = new AuditActorOptionResponse(user.getUserId(), user.getEmail(), user.getRole().name());
        AuditFilterOptionsResponse options = new AuditFilterOptionsResponse(List.of(option), List.of(), List.of(actor));
        StudentDataDeletionEligibilityResponse eligibility = StudentDataDeletionEligibilityResponse.builder()
                .studentId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .canAnonymize(true)
                .blockers(List.of())
                .build();

        assertThat(userResponse.getEmail()).contains("***");
        assertThat(auditLogResponse.getAction()).isEqualTo(AuditAction.LOGIN);
        assertThat(auditLogResponse.getActionLabel()).isEqualTo("Login");
        assertThat(option.getLabel()).isEqualTo("Login");
        assertThat(actor.getLabel()).contains("ADMIN");
        assertThat(options.getActors()).hasSize(1);
        assertThat(eligibility.isCanAnonymize()).isTrue();
    }
}
