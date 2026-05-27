package com.gymiq.repository;

import com.gymiq.entity.Presence;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PresenceRepository extends JpaRepository<Presence, Integer> {

    Page<Presence> findByStudentStudentId(Integer studentId, Pageable pageable);

    Optional<Presence> findByStudentStudentIdAndCheckOutAtIsNull(Integer studentId);

    Optional<Presence> findFirstByStudentStudentIdOrderByCheckInAtDesc(Integer studentId);

    Page<Presence> findByCheckInAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    @Query("""
            SELECT COUNT(p)
            FROM Presence p
            WHERE p.checkInAt >= :startDate
              AND p.checkInAt < :endDate
            """)
    long countCheckInsBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    long countByCheckOutAtIsNull();
}
