package com.gymiq.repository;

import com.gymiq.entity.Payment;
import com.gymiq.entity.Enrollment.EnrollmentStatus;
import com.gymiq.entity.Payment.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    List<Payment> findByEnrollmentEnrollmentId(Integer enrollmentId);

    List<Payment> findByEnrollmentEnrollmentIdOrderByDueDateDesc(Integer enrollmentId);

    Page<Payment> findByEnrollmentEnrollmentId(Integer enrollmentId, Pageable pageable);

    List<Payment> findByEnrollmentStudentStudentId(Integer studentId);

    Page<Payment> findByEnrollmentStudentStudentId(Integer studentId, Pageable pageable);

    List<Payment> findByStatus(PaymentStatus status);

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    List<Payment> findByStatusAndDueDateBefore(PaymentStatus status, LocalDate date);

    long countByStatusAndDueDateBetween(PaymentStatus status, LocalDate startDate, LocalDate endDate);

    @Query("""
            SELECT SUM(p.amount)
            FROM Payment p
            WHERE p.status = :status
              AND p.dueDate BETWEEN :startDate AND :endDate
            """)
    BigDecimal sumAmountByStatusAndDueDateBetween(
            @Param("status") PaymentStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    boolean existsByEnrollmentEnrollmentIdAndDueDate(Integer enrollmentId, LocalDate dueDate);

    Optional<Payment> findTopByEnrollmentEnrollmentIdOrderByDueDateDesc(Integer enrollmentId);

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
