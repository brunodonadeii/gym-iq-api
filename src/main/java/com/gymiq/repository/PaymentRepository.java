package com.gymiq.repository;

import com.gymiq.entity.Payment;
import com.gymiq.entity.Enrollment.EnrollmentStatus;
import com.gymiq.entity.Payment.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("""
            SELECT DISTINCT p.enrollment.student.studentId
            FROM Payment p
            WHERE p.enrollment.status = :activeStatus
              AND p.enrollment.student.user.active = true
              AND (
                    p.status = :overdueStatus
                    OR (p.status = :pendingStatus AND p.dueDate < :today)
              )
            """)
    List<Integer> findActiveStudentIdsWithOverduePayments(
            @Param("activeStatus") EnrollmentStatus activeStatus,
            @Param("overdueStatus") PaymentStatus overdueStatus,
            @Param("pendingStatus") PaymentStatus pendingStatus,
            @Param("today") LocalDate today);
}
