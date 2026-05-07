package com.gymiq.repository;

import com.gymiq.entity.RetentionAlert;
import com.gymiq.entity.RetentionAlert.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RetentionAlertRepository extends JpaRepository<RetentionAlert, Integer> {

    List<RetentionAlert> findByStatusOrderByRiskScoreDescUpdatedAtDesc(AlertStatus status);

    List<RetentionAlert> findByStudentStudentIdOrderByUpdatedAtDesc(Integer studentId);

    Optional<RetentionAlert> findByStudentStudentIdAndStatus(Integer studentId, AlertStatus status);
}
