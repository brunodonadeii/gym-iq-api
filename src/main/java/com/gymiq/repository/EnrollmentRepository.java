package com.gymiq.repository;

import java.util.UUID;

import com.gymiq.entity.Enrollment;
import com.gymiq.entity.Enrollment.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    @Override
    @EntityGraph(attributePaths = {
            "student",
            "student.user",
            "plan"
    })
    Page<Enrollment> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {
            "student",
            "student.user",
            "plan"
    })
    Optional<Enrollment> findById(UUID id);

    @EntityGraph(attributePaths = {
            "student",
            "student.user",
            "plan"
    })
    Page<Enrollment> findByStudentStudentId(UUID studentId, Pageable pageable);

    @EntityGraph(attributePaths = {
            "student",
            "student.user",
            "plan"
    })
    List<Enrollment> findByStatus(EnrollmentStatus status);

    @EntityGraph(attributePaths = {
            "student",
            "student.user",
            "plan"
    })
    Page<Enrollment> findByStatus(EnrollmentStatus status, Pageable pageable);

    long countByStatus(EnrollmentStatus status);

    @Query("""
            SELECT COUNT(DISTINCT e.student.studentId)
            FROM Enrollment e
            WHERE e.startDate <= :referenceDate
              AND (
                    e.status = :activeStatus
                    OR (e.status = :canceledStatus AND e.canceledAt >= :referenceDateTime)
              )
            """)
    long countActiveCustomersAtDate(
            @Param("referenceDate") LocalDate referenceDate,
            @Param("referenceDateTime") LocalDateTime referenceDateTime,
            @Param("activeStatus") EnrollmentStatus activeStatus,
            @Param("canceledStatus") EnrollmentStatus canceledStatus);

    @Query("""
            SELECT COUNT(DISTINCT e.student.studentId)
            FROM Enrollment e
            WHERE e.status = :canceledStatus
              AND e.canceledAt >= :startDateTime
              AND e.canceledAt < :endDateTime
            """)
    long countCanceledCustomersBetween(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            @Param("canceledStatus") EnrollmentStatus canceledStatus);

    @Query("""
            SELECT COUNT(e)
            FROM Enrollment e
            WHERE e.status = :canceledStatus
              AND e.canceledAt >= :startDateTime
              AND e.canceledAt < :endDateTime
            """)
    long countCanceledEnrollmentsBetween(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            @Param("canceledStatus") EnrollmentStatus canceledStatus);

    @Query("""
            SELECT COUNT(DISTINCT e.student.studentId)
            FROM Enrollment e
            WHERE e.status = :status
              AND e.student.user.active = true
            """)
    long countDistinctStudentsByStatus(@Param("status") EnrollmentStatus status);

    @Query("""
            SELECT COUNT(DISTINCT e.student.studentId)
            FROM Enrollment e
            WHERE e.status = :status
              AND e.student.user.active = true
              AND e.startDate <= :referenceDate
              AND (e.endDate IS NULL OR e.endDate >= :referenceDate)
            """)
    long countActiveStudentsForCurrentOperation(
            @Param("status") EnrollmentStatus status,
            @Param("referenceDate") LocalDate referenceDate);

    @EntityGraph(attributePaths = {
            "student",
            "student.user",
            "plan"
    })
    Optional<Enrollment> findByStudentStudentIdAndStatus(UUID studentId, EnrollmentStatus status);

    @EntityGraph(attributePaths = {
            "student",
            "student.user",
            "plan"
    })
    Optional<Enrollment> findTopByStudentStudentIdOrderByStartDateDescCreatedAtDesc(UUID studentId);

    boolean existsByStudentStudentIdAndStatus(UUID studentId, EnrollmentStatus status);

    boolean existsByStudentStudentIdAndStatusIn(UUID studentId, Collection<EnrollmentStatus> statuses);

    @Query("""
            SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
            FROM Enrollment e
            WHERE e.student.studentId = :studentId
              AND e.status = :status
              AND e.startDate <= :referenceDate
              AND (e.endDate IS NULL OR e.endDate >= :referenceDate)
            """)
    boolean existsActiveEnrollmentForStudent(
            @Param("studentId") UUID studentId,
            @Param("status") EnrollmentStatus status,
            @Param("referenceDate") LocalDate referenceDate);

    @Query("""
            SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
            FROM Enrollment e
            WHERE e.student.user.userId = :userId
              AND e.status = :status
              AND e.startDate <= :referenceDate
              AND (e.endDate IS NULL OR e.endDate >= :referenceDate)
            """)
    boolean existsActiveEnrollmentForStudentUser(
            @Param("userId") UUID userId,
            @Param("status") EnrollmentStatus status,
            @Param("referenceDate") LocalDate referenceDate);

    boolean existsByPlanPlanId(Integer planId);

    @Query("SELECT e FROM Enrollment e WHERE e.status = 'ACTIVE' " +
            "AND e.endDate BETWEEN :today AND :limit")
    List<Enrollment> findExpiringBetween(@Param("today") LocalDate today,
                                         @Param("limit") LocalDate limit);

    @Query("""
            SELECT COUNT(e)
            FROM Enrollment e
            WHERE e.status = :status
              AND e.endDate BETWEEN :startDate AND :endDate
            """)
    long countByStatusAndEndDateBetween(
            @Param("status") EnrollmentStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT COUNT(DISTINCT e.student.studentId)
            FROM Enrollment e
            WHERE e.status = 'ACTIVE'
              AND e.student.user.active = true
              AND NOT EXISTS (
                    SELECT p.presenceId
                    FROM Presence p
                    WHERE p.student = e.student
                      AND p.checkInAt >= :limitDate
              )
            """)
    long countActiveStudentsWithoutCheckInSince(@Param("limitDate") LocalDateTime limitDate);
}
