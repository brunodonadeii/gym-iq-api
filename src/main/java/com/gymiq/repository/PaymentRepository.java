package com.gymiq.repository;

import com.gymiq.entity.Payment;
import com.gymiq.entity.Payment.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    List<Payment> findByEnrollmentEnrollmentId(Integer enrollmentId);

    List<Payment> findByEnrollmentStudentStudentId(Integer studentId);

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByStatusAndDueDateBefore(PaymentStatus status, LocalDate date);

    long countByEnrollmentStudentStudentIdAndStatus(Integer studentId, PaymentStatus status);

    long countByEnrollmentStudentStudentIdAndStatusAndDueDateBefore(
            Integer studentId,
            PaymentStatus status,
            LocalDate date);
}
