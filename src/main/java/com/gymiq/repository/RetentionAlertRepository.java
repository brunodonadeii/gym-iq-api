package com.gymiq.repository;

import java.util.UUID;

import com.gymiq.entity.Enrollment.EnrollmentStatus;
import com.gymiq.entity.RetentionAlert;
import com.gymiq.entity.RetentionAlert.AlertStatus;
import com.gymiq.entity.RetentionAlert.RiskLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface RetentionAlertRepository extends JpaRepository<RetentionAlert, UUID> {

    Page<RetentionAlert> findByStatus(AlertStatus status, Pageable pageable);

    long countByStatus(AlertStatus status);

    @Query("""
            SELECT r
            FROM RetentionAlert r
            WHERE r.status = :status
              AND r.student.user.active = true
              AND EXISTS (
                    SELECT e.enrollmentId
                    FROM Enrollment e
                    WHERE e.student = r.student
                      AND e.status = :activeStatus
              )
            """)
    Page<RetentionAlert> findOpenAlertsForActiveStudents(
            @Param("status") AlertStatus status,
            @Param("activeStatus") EnrollmentStatus activeStatus,
            Pageable pageable);

    @Query("""
            SELECT COUNT(r)
            FROM RetentionAlert r
            WHERE r.status = :status
              AND r.student.user.active = true
              AND EXISTS (
                    SELECT e.enrollmentId
                    FROM Enrollment e
                    WHERE e.student = r.student
                      AND e.status = :activeStatus
              )
            """)
    long countOpenAlertsForActiveStudents(
            @Param("status") AlertStatus status,
            @Param("activeStatus") EnrollmentStatus activeStatus);

    long countByStatusAndRiskLevel(AlertStatus status, RiskLevel riskLevel);

    @Query("""
            SELECT COUNT(r)
            FROM RetentionAlert r
            WHERE r.status = :status
              AND r.riskLevel = :riskLevel
              AND r.student.user.active = true
              AND EXISTS (
                    SELECT e.enrollmentId
                    FROM Enrollment e
                    WHERE e.student = r.student
                      AND e.status = :activeStatus
              )
            """)
    long countOpenAlertsForActiveStudentsByRiskLevel(
            @Param("status") AlertStatus status,
            @Param("activeStatus") EnrollmentStatus activeStatus,
            @Param("riskLevel") RiskLevel riskLevel);

    @Query("SELECT AVG(r.riskScore) FROM RetentionAlert r WHERE r.status = :status")
    Optional<Double> averageRiskScoreByStatus(@Param("status") AlertStatus status);

    @Query("""
            SELECT AVG(r.riskScore)
            FROM RetentionAlert r
            WHERE r.status = :status
              AND r.student.user.active = true
              AND EXISTS (
                    SELECT e.enrollmentId
                    FROM Enrollment e
                    WHERE e.student = r.student
                      AND e.status = :activeStatus
              )
            """)
    Optional<Double> averageRiskScoreForActiveStudents(
            @Param("status") AlertStatus status,
            @Param("activeStatus") EnrollmentStatus activeStatus);

    Page<RetentionAlert> findByStudentStudentId(UUID studentId, Pageable pageable);

    Optional<RetentionAlert> findByStudentStudentIdAndStatus(UUID studentId, AlertStatus status);
}
