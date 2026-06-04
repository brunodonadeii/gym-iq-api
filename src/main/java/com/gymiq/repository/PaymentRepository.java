package com.gymiq.repository;

import java.util.UUID;

import com.gymiq.entity.Payment;
import com.gymiq.entity.Enrollment.EnrollmentStatus;
import com.gymiq.entity.Payment.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @Override
    @EntityGraph(attributePaths = {
            "enrollment",
            "enrollment.student",
            "enrollment.student.user",
            "enrollment.plan"
    })
    Page<Payment> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {
            "enrollment",
            "enrollment.student",
            "enrollment.student.user",
            "enrollment.plan"
    })
    List<Payment> findByEnrollmentEnrollmentId(UUID enrollmentId);

    @EntityGraph(attributePaths = {
            "enrollment",
            "enrollment.student",
            "enrollment.student.user",
            "enrollment.plan"
    })
    List<Payment> findByEnrollmentEnrollmentIdOrderByDueDateDesc(UUID enrollmentId);

    @EntityGraph(attributePaths = {
            "enrollment",
            "enrollment.student",
            "enrollment.student.user",
            "enrollment.plan"
    })
    Page<Payment> findByEnrollmentEnrollmentId(UUID enrollmentId, Pageable pageable);

    @EntityGraph(attributePaths = {
            "enrollment",
            "enrollment.student",
            "enrollment.student.user",
            "enrollment.plan"
    })
    List<Payment> findByEnrollmentStudentStudentId(UUID studentId);

    @EntityGraph(attributePaths = {
            "enrollment",
            "enrollment.student",
            "enrollment.student.user",
            "enrollment.plan"
    })
    Page<Payment> findByEnrollmentStudentStudentId(UUID studentId, Pageable pageable);

    @EntityGraph(attributePaths = {
            "enrollment",
            "enrollment.student",
            "enrollment.student.user",
            "enrollment.plan"
    })
    List<Payment> findByStatus(PaymentStatus status);

    @EntityGraph(attributePaths = {
            "enrollment",
            "enrollment.student",
            "enrollment.student.user",
            "enrollment.plan"
    })
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

    boolean existsByEnrollmentEnrollmentIdAndDueDate(UUID enrollmentId, LocalDate dueDate);

    Optional<Payment> findTopByEnrollmentEnrollmentIdOrderByDueDateDesc(UUID enrollmentId);

    long countByEnrollmentStudentStudentIdAndStatus(UUID studentId, PaymentStatus status);

    boolean existsByEnrollmentStudentStudentIdAndStatusIn(
            UUID studentId,
            Collection<PaymentStatus> statuses);

    long countByEnrollmentStudentStudentIdAndStatusAndDueDateBefore(
            UUID studentId,
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
    List<UUID> findActiveStudentIdsWithOverduePayments(
            @Param("activeStatus") EnrollmentStatus activeStatus,
            @Param("overdueStatus") PaymentStatus overdueStatus,
            @Param("pendingStatus") PaymentStatus pendingStatus,
            @Param("today") LocalDate today);
}
