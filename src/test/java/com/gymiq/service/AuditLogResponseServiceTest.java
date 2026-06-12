package com.gymiq.service;

import com.gymiq.dto.response.AuditLogResponse;
import com.gymiq.entity.AuditLog;
import com.gymiq.entity.Enrollment;
import com.gymiq.entity.Student;
import com.gymiq.entity.User;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogResponseServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private InstructorRepository instructorRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PresenceRepository presenceRepository;

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private WorkoutSheetRepository workoutSheetRepository;

    @Mock
    private WorkoutBlockRepository workoutBlockRepository;

    @Mock
    private WorkoutSheetExerciseRepository workoutSheetExerciseRepository;

    @Mock
    private RetentionAlertRepository retentionAlertRepository;

    @Test
    void toResponseShouldExposeActionLabelAndEnrollmentResourceLabel() {
        AuditLogResponseService service = service();

        UUID enrollmentId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        User user = User.builder()
                .name("Marcos Vinicius")
                .email("student@gymiq.com")
                .role(User.Role.STUDENT)
                .build();
        Student student = Student.builder()
                .user(user)
                .build();
        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .build();
        AuditLog log = AuditLog.builder()
                .auditLogId(10L)
                .actorEmail("admin@gymiq.com")
                .actorRole("ADMIN")
                .action(AuditAction.CREATE_ENROLLMENT)
                .resourceType(ResourceType.ENROLLMENT)
                .resourceId(enrollmentId.toString())
                .description("Criou matrícula")
                .createdAt(LocalDateTime.now())
                .build();

        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        AuditLogResponse response = service.toResponse(log);

        assertThat(response.getActionLabel()).isEqualTo("Criação de matrícula");
        assertThat(response.getActorEmail()).isEqualTo("admin@gymiq.com");
        assertThat(response.getActorLabel()).isEqualTo("admin@gymiq.com");
        assertThat(response.getResourceLabel()).isEqualTo("Matrícula de Marcos Vinicius");
    }

    @Test
    void toResponseShouldUseSystemLabelsForJobLogs() {
        AuditLogResponseService service = service();
        AuditLog log = AuditLog.builder()
                .action(AuditAction.GENERATE_MONTHLY_PAYMENTS)
                .resourceType(ResourceType.JOB)
                .description("Gerou pagamentos")
                .createdAt(LocalDateTime.now())
                .build();

        AuditLogResponse response = service.toResponse(log);

        assertThat(response.getActorEmail()).isNull();
        assertThat(response.getActorLabel()).isEqualTo("Sistema");
        assertThat(response.getResourceLabel()).isEqualTo("Rotina automática");
        assertThat(response.getActionLabel()).isEqualTo("Geração de pagamentos mensais");
    }

    @Test
    void toResponseShouldHideActorEmailForStudentActors() {
        AuditLogResponseService service = service();
        UUID actorId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        User studentUser = User.builder()
                .userId(actorId)
                .name("Marcos Vinicius")
                .email("student@gymiq.com")
                .role(User.Role.STUDENT)
                .build();
        AuditLog log = AuditLog.builder()
                .actorUserId(actorId)
                .actorEmail("student@gymiq.com")
                .actorRole("STUDENT")
                .action(AuditAction.SELF_CHECK_IN)
                .resourceType(ResourceType.PRESENCE)
                .resourceId("presence-id")
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(actorId)).thenReturn(Optional.of(studentUser));

        AuditLogResponse response = service.toResponse(log);

        assertThat(response.getActorEmail()).isNull();
        assertThat(response.getActorLabel()).isEqualTo("Marcos Vinicius");
    }

    private AuditLogResponseService service() {
        return new AuditLogResponseService(
                userRepository,
                studentRepository,
                instructorRepository,
                planRepository,
                enrollmentRepository,
                paymentRepository,
                presenceRepository,
                exerciseRepository,
                workoutSheetRepository,
                workoutBlockRepository,
                workoutSheetExerciseRepository,
                retentionAlertRepository);
    }
}
