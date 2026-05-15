package com.gymiq.repository;

import com.gymiq.entity.RetentionAlert;
import com.gymiq.entity.RetentionAlert.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RetentionAlertRepository extends JpaRepository<RetentionAlert, Integer> {

    Page<RetentionAlert> findByStatus(AlertStatus status, Pageable pageable);

    Page<RetentionAlert> findByStudentStudentId(Integer studentId, Pageable pageable);

    Optional<RetentionAlert> findByStudentStudentIdAndStatus(Integer studentId, AlertStatus status);
}
