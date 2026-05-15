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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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

        when(studentRepository.findById(student.getStudentId())).thenReturn(Optional.of(student));
        when(enrollmentRepository.existsByStudentStudentIdAndStatus(
                student.getStudentId(), Enrollment.EnrollmentStatus.ACTIVE)).thenReturn(true);
        when(presenceRepository.findFirstByStudentStudentIdOrderByCheckInAtDesc(student.getStudentId()))
                .thenReturn(Optional.empty());
        when(paymentRepository.countByEnrollmentStudentStudentIdAndStatus(
                student.getStudentId(), Payment.PaymentStatus.OVERDUE)).thenReturn(1L);
        when(paymentRepository.countByEnrollmentStudentStudentIdAndStatusAndDueDateBefore(
                eq(student.getStudentId()), eq(Payment.PaymentStatus.PENDING), any(LocalDate.class))).thenReturn(0L);
        when(retentionAlertRepository.findByStudentStudentIdAndStatus(
                student.getStudentId(), RetentionAlert.AlertStatus.OPEN)).thenReturn(Optional.empty());

        RetentionAlertResponse response = retentionAlertService.generateForStudent(student.getStudentId());

        assertThat(response.getStatus()).isEqualTo("OPEN");
        assertThat(response.getOverduePayments()).isEqualTo(1);
        assertThat(response.getRiskScore()).isGreaterThanOrEqualTo(20);
        verify(retentionAlertRepository).save(any(RetentionAlert.class));
    }

    @Test
    void generateForOverdueStudentsShouldGenerateAlertForEachOverdueStudent() {
        when(paymentRepository.findActiveStudentIdsWithOverduePayments(
                Enrollment.EnrollmentStatus.ACTIVE,
                Payment.PaymentStatus.OVERDUE,
                Payment.PaymentStatus.PENDING,
                LocalDate.now())).thenReturn(List.of(1, 2));
        when(studentRepository.findById(anyInt())).thenAnswer(invocation ->
                Optional.of(activeStudentWithId(invocation.getArgument(0))));
        when(enrollmentRepository.existsByStudentStudentIdAndStatus(
                anyInt(), eq(Enrollment.EnrollmentStatus.ACTIVE))).thenReturn(true);
        when(presenceRepository.findFirstByStudentStudentIdOrderByCheckInAtDesc(anyInt()))
                .thenReturn(Optional.empty());
        when(paymentRepository.countByEnrollmentStudentStudentIdAndStatus(
                anyInt(), eq(Payment.PaymentStatus.OVERDUE))).thenReturn(1L);
        when(paymentRepository.countByEnrollmentStudentStudentIdAndStatusAndDueDateBefore(
                anyInt(), eq(Payment.PaymentStatus.PENDING), any(LocalDate.class))).thenReturn(1L);
        when(retentionAlertRepository.findByStudentStudentIdAndStatus(
                anyInt(), eq(RetentionAlert.AlertStatus.OPEN))).thenReturn(Optional.empty());

        List<RetentionAlertResponse> responses = retentionAlertService.generateForOverdueStudents();

        assertThat(responses).hasSize(2);
        assertThat(responses).allSatisfy(response -> {
            assertThat(response.getStatus()).isEqualTo("OPEN");
            assertThat(response.getOverduePayments()).isEqualTo(2);
        });
    }

    private Student activeStudentWithId(Integer studentId) {
        Student student = TestDataFactory.activeStudent();
        student.setStudentId(studentId);
        student.getUser().setUserId(studentId + 10);
        student.getUser().setEmail("student" + studentId + "@gymiq.com");
        return student;
    }
}
