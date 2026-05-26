package com.gymiq.repository;

import com.gymiq.entity.Enrollment;
import com.gymiq.entity.Enrollment.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Integer> {

    Page<Enrollment> findByStudentStudentId(Integer studentId, Pageable pageable);

    List<Enrollment> findByStatus(EnrollmentStatus status);

    @Query("""
            SELECT COUNT(DISTINCT e.student.studentId)
            FROM Enrollment e
            WHERE e.status = :status
              AND e.student.user.active = true
            """)
    long countDistinctStudentsByStatus(@Param("status") EnrollmentStatus status);

    Optional<Enrollment> findByStudentStudentIdAndStatus(Integer studentId, EnrollmentStatus status);

    boolean existsByStudentStudentIdAndStatus(Integer studentId, EnrollmentStatus status);

    boolean existsByPlanPlanId(Integer planId);

    @Query("SELECT e FROM Enrollment e WHERE e.status = 'ACTIVE' " +
            "AND e.endDate BETWEEN :today AND :limit")
    List<Enrollment> findExpiringBetween(@Param("today") LocalDate today,
                                         @Param("limit") LocalDate limit);

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
