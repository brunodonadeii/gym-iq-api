package com.gymiq.service;

import com.gymiq.dto.request.EnrollStudentRequest;
import com.gymiq.dto.response.EnrollmentResponse;
import com.gymiq.entity.Enrollment;
import com.gymiq.entity.Plan;
import com.gymiq.entity.Student;
import com.gymiq.exception.BusinessException;
import com.gymiq.repository.EnrollmentRepository;
import com.gymiq.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private StudentService studentService;

    @Mock
    private PlanService planService;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private EnrollmentService enrollmentService;

    @Test
    void enrollShouldCreateActiveEnrollmentAndFirstPayment() {
        Student student = TestDataFactory.activeStudent();
        Plan plan = TestDataFactory.activePlan();
        EnrollStudentRequest request = new EnrollStudentRequest();
        request.setStudentId(student.getStudentId());
        request.setPlanId(plan.getPlanId());
        request.setStartDate(LocalDate.of(2026, 5, 15));

        when(studentService.findEntityById(student.getStudentId())).thenReturn(student);
        when(planService.findEntityById(plan.getPlanId())).thenReturn(plan);
        when(enrollmentRepository.existsByStudentStudentIdAndStatus(
                student.getStudentId(), Enrollment.EnrollmentStatus.ACTIVE)).thenReturn(false);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> {
            Enrollment enrollment = invocation.getArgument(0);
            enrollment.setEnrollmentId(3);
            return enrollment;
        });

        EnrollmentResponse response = enrollmentService.enroll(request);

        assertThat(response.getEnrollmentId()).isEqualTo(3);
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getEndDate()).isEqualTo(request.getStartDate().plusDays(plan.getDurationDays()));
        verify(paymentService).createFirstPaymentForEnrollment(any(Enrollment.class));
    }

    @Test
    void enrollShouldRejectStudentWithActiveEnrollment() {
        Student student = TestDataFactory.activeStudent();
        Plan plan = TestDataFactory.activePlan();
        EnrollStudentRequest request = new EnrollStudentRequest();
        request.setStudentId(student.getStudentId());
        request.setPlanId(plan.getPlanId());

        when(studentService.findEntityById(student.getStudentId())).thenReturn(student);
        when(planService.findEntityById(plan.getPlanId())).thenReturn(plan);
        when(enrollmentRepository.existsByStudentStudentIdAndStatus(
                student.getStudentId(), Enrollment.EnrollmentStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> enrollmentService.enroll(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("matrícula ativa");
    }

    @Test
    void changeStatusShouldRejectInvalidTransition() {
        Enrollment enrollment = TestDataFactory.activeEnrollment();

        when(enrollmentRepository.findById(enrollment.getEnrollmentId())).thenReturn(Optional.of(enrollment));

        assertThatThrownBy(() -> enrollmentService.changeStatus(
                enrollment.getEnrollmentId(), Enrollment.EnrollmentStatus.ACTIVE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Transição");
    }

    @Test
    void renewShouldCancelOldEnrollmentAndCreateNewOne() {
        Enrollment oldEnrollment = TestDataFactory.activeEnrollment();

        when(enrollmentRepository.findById(oldEnrollment.getEnrollmentId())).thenReturn(Optional.of(oldEnrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> {
            Enrollment enrollment = invocation.getArgument(0);
            if (enrollment.getEnrollmentId() == null) {
                enrollment.setEnrollmentId(4);
            }
            return enrollment;
        });

        EnrollmentResponse response = enrollmentService.renew(oldEnrollment.getEnrollmentId(), null);

        assertThat(oldEnrollment.getStatus()).isEqualTo(Enrollment.EnrollmentStatus.CANCELED);
        assertThat(response.getEnrollmentId()).isEqualTo(4);
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        verify(paymentService).createFirstPaymentForEnrollment(any(Enrollment.class));
    }
}
