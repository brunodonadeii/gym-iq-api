package com.gymiq.repository;

import com.gymiq.entity.Presence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PresenceRepository extends JpaRepository<Presence, Integer> {

    List<Presence> findByStudentStudentIdOrderByCheckInAtDesc(Integer studentId);

    Optional<Presence> findByStudentStudentIdAndCheckOutAtIsNull(Integer studentId);

    List<Presence> findByCheckInAtBetweenOrderByCheckInAtDesc(LocalDateTime start, LocalDateTime end);
}
