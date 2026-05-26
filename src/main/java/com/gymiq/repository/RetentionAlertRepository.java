package com.gymiq.repository;

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
public interface RetentionAlertRepository extends JpaRepository<RetentionAlert, Integer> {

    Page<RetentionAlert> findByStatus(AlertStatus status, Pageable pageable);

    long countByStatus(AlertStatus status);

    long countByStatusAndRiskLevel(AlertStatus status, RiskLevel riskLevel);

    @Query("SELECT AVG(r.riskScore) FROM RetentionAlert r WHERE r.status = :status")
    Optional<Double> averageRiskScoreByStatus(@Param("status") AlertStatus status);

    Page<RetentionAlert> findByStudentStudentId(Integer studentId, Pageable pageable);

    Optional<RetentionAlert> findByStudentStudentIdAndStatus(Integer studentId, AlertStatus status);
}
