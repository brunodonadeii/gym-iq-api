package com.gymiq.repository;

import com.gymiq.entity.Enrollment;
import com.gymiq.entity.Enrollment.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Integer> {

    List<Enrollment> findByStudentStudentId(Integer studentId);

    List<Enrollment> findByStatus(EnrollmentStatus status);

    Optional<Enrollment> findByStudentStudentIdAndStatus(Integer studentId, EnrollmentStatus status);

    boolean existsByStudentStudentIdAndStatus(Integer studentId, EnrollmentStatus status);

    @Query("SELECT e FROM Enrollment e WHERE e.status = 'ACTIVE' " +
            "AND e.endDate BETWEEN :today AND :limit")
    List<Enrollment> findExpiringBetween(@Param("today") LocalDate today,
                                         @Param("limit") LocalDate limit);
}