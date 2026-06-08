package com.gymiq.service;

import com.gymiq.dto.response.RetentionAlertResponse;
import com.gymiq.entity.Enrollment;
import com.gymiq.entity.Payment;
import com.gymiq.entity.RetentionAlert;
import com.gymiq.entity.Student;
import com.gymiq.repository.EnrollmentRepository;
import com.gymiq.repository.PaymentRepository;
import com.gymiq.repository.PresenceRepository;
import com.gymiq.repository.RetentionAlertRepository;
import com.gymiq.repository.StudentRepository;
import com.gymiq.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetentionAlertServiceTest {

    @Mock
    private RetentionAlertRepository retentionAlertRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private PresenceRepository presenceRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private RetentionAlertService retentionAlertService;

    @Test
    void generateForStudentShouldCreateOpenAlertWhenStudentHasOverduePayment() {
        Student student = TestDataFactory.activeStudent();
        Enrollment enrollment = TestDataFactory.activeEnrollment();

        when(studentRepository.findById(student.getStudentId())).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByStudentStudentIdAndStatus(
                student.getStudentId(), Enrollment.EnrollmentStatus.ACTIVE)).thenReturn(Optional.of(enrollment));
        when(presenceRepository.findFirstByStudentStudentIdAndCheckInAtGreaterThanEqualOrderByCheckInAtDesc(
                eq(student.getStudentId()), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        when(presenceRepository.countByStudentStudentIdAndCheckInAtGreaterThanEqualAndCheckInAtLessThan(
                eq(student.getStudentId()), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);
        when(paymentRepository.countByEnrollmentStudentStudentIdAndStatus(
                student.getStudentId(), Payment.PaymentStatus.OVERDUE)).thenReturn(1L);
        when(paymentRepository.countByEnrollmentStudentStudentIdAndStatusAndDueDateBefore(
                eq(student.getStudentId()), eq(Payment.PaymentStatus.PENDING), any(LocalDate.class))).thenReturn(0L);
        when(retentionAlertRepository.findByStudentStudentIdAndStatus(
                student.getStudentId(), RetentionAlert.AlertStatus.OPEN)).thenReturn(Optional.empty());

        Optional<RetentionAlertResponse> response = retentionAlertService.generateForStudent(student.getStudentId());

        assertThat(response).isPresent();
        assertThat(response.get().getStatus()).isEqualTo("OPEN");
        assertThat(response.get().getOverduePayments()).isEqualTo(1);
        assertThat(response.get().getRiskScore()).isGreaterThanOrEqualTo(15);
        verify(retentionAlertRepository).save(any(RetentionAlert.class));
    }

    @Test
    void generateForOverdueStudentsShouldGenerateAlertForEachOverdueStudent() {
        UUID firstStudentId = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID secondStudentId = UUID.fromString("00000000-0000-0000-0000-000000000102");
        when(paymentRepository.findActiveStudentIdsWithOverduePayments(
                Enrollment.EnrollmentStatus.ACTIVE,
                Payment.PaymentStatus.OVERDUE,
                Payment.PaymentStatus.PENDING,
                LocalDate.now())).thenReturn(List.of(firstStudentId, secondStudentId));
        when(studentRepository.findById(any(UUID.class))).thenAnswer(invocation ->
                Optional.of(activeStudentWithId(invocation.getArgument(0))));
        when(enrollmentRepository.findByStudentStudentIdAndStatus(
                any(UUID.class), eq(Enrollment.EnrollmentStatus.ACTIVE))).thenAnswer(invocation -> {
            Enrollment enrollment = TestDataFactory.activeEnrollment();
            enrollment.setStudent(activeStudentWithId(invocation.getArgument(0)));
            return Optional.of(enrollment);
        });
        when(presenceRepository.findFirstByStudentStudentIdAndCheckInAtGreaterThanEqualOrderByCheckInAtDesc(
                any(UUID.class), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        when(presenceRepository.countByStudentStudentIdAndCheckInAtGreaterThanEqualAndCheckInAtLessThan(
                any(UUID.class), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);
        when(paymentRepository.countByEnrollmentStudentStudentIdAndStatus(
                any(UUID.class), eq(Payment.PaymentStatus.OVERDUE))).thenReturn(1L);
        when(paymentRepository.countByEnrollmentStudentStudentIdAndStatusAndDueDateBefore(
                any(UUID.class), eq(Payment.PaymentStatus.PENDING), any(LocalDate.class))).thenReturn(1L);
        when(retentionAlertRepository.findByStudentStudentIdAndStatus(
                any(UUID.class), eq(RetentionAlert.AlertStatus.OPEN))).thenReturn(Optional.empty());

        List<RetentionAlertResponse> responses = retentionAlertService.generateForOverdueStudents();

        assertThat(responses).hasSize(2);
        assertThat(responses).allSatisfy(response -> {
            assertThat(response.getStatus()).isEqualTo("OPEN");
            assertThat(response.getOverduePayments()).isEqualTo(2);
        });
    }

    private Student activeStudentWithId(UUID studentId) {
        Student student = TestDataFactory.activeStudent();
        student.setStudentId(studentId);
        student.getUser().setUserId(UUID.randomUUID());
        student.getUser().setEmail("student" + studentId + "@gymiq.com");
        return student;
    }
}
