package com.gymiq.service;

import com.gymiq.dto.response.RetentionAlertJobStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class RetentionAlertJobService {

    private final RetentionAlertService retentionAlertService;
    private final Executor retentionJobExecutor;
    private final Object lock = new Object();

    private RetentionJobSnapshot currentJob = RetentionJobSnapshot.idle();

    public RetentionAlertJobService(
            RetentionAlertService retentionAlertService,
            @Qualifier("retentionJobExecutor") Executor retentionJobExecutor) {
        this.retentionAlertService = retentionAlertService;
        this.retentionJobExecutor = retentionJobExecutor;
    }

    public RetentionAlertJobStatusResponse startGenerateActiveStudentsJob() {
        RetentionJobSnapshot snapshot;

        synchronized (lock) {
            if (currentJob.status() == RetentionJobStatus.RUNNING) {
                return toResponse(currentJob, "Analise de retencao ja esta em processamento.");
            }

            snapshot = RetentionJobSnapshot.running(UUID.randomUUID(), LocalDateTime.now());
            currentJob = snapshot;
        }

        retentionJobExecutor.execute(() -> runGenerateActiveStudentsJob(snapshot.jobId()));
        return toResponse(snapshot, "Analise de retencao iniciada.");
    }

    public RetentionAlertJobStatusResponse getLatestJobStatus() {
        synchronized (lock) {
            return toResponse(currentJob, resolveStatusMessage(currentJob.status()));
        }
    }

    private void runGenerateActiveStudentsJob(UUID jobId) {
        try {
            int generatedAlerts = retentionAlertService.generateForActiveStudents().size();

            synchronized (lock) {
                currentJob = currentJob.completed(generatedAlerts, LocalDateTime.now());
            }

            log.info("Retention async job completed: jobId={}, generatedAlerts={}", jobId, generatedAlerts);
        } catch (Exception exception) {
            synchronized (lock) {
                currentJob = currentJob.failed(exception.getMessage(), LocalDateTime.now());
            }

            log.error("Retention async job failed: jobId={}", jobId, exception);
        }
    }

    private RetentionAlertJobStatusResponse toResponse(RetentionJobSnapshot snapshot, String message) {
        return RetentionAlertJobStatusResponse.builder()
                .jobId(snapshot.jobId())
                .status(snapshot.status().name())
                .message(message)
                .generatedAlerts(snapshot.generatedAlerts())
                .startedAt(snapshot.startedAt())
                .finishedAt(snapshot.finishedAt())
                .errorMessage(snapshot.errorMessage())
                .build();
    }

    private String resolveStatusMessage(RetentionJobStatus status) {
        return switch (status) {
            case IDLE -> "Nenhuma analise de retencao foi iniciada.";
            case RUNNING -> "Analise de retencao em processamento.";
            case COMPLETED -> "Ultima analise de retencao concluida.";
            case FAILED -> "Ultima analise de retencao falhou.";
        };
    }

    private enum RetentionJobStatus {
        IDLE,
        RUNNING,
        COMPLETED,
        FAILED
    }

    private record RetentionJobSnapshot(
            UUID jobId,
            RetentionJobStatus status,
            Integer generatedAlerts,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            String errorMessage) {

        static RetentionJobSnapshot idle() {
            return new RetentionJobSnapshot(null, RetentionJobStatus.IDLE, 0, null, null, null);
        }

        static RetentionJobSnapshot running(UUID jobId, LocalDateTime startedAt) {
            return new RetentionJobSnapshot(jobId, RetentionJobStatus.RUNNING, 0, startedAt, null, null);
        }

        RetentionJobSnapshot completed(Integer generatedAlerts, LocalDateTime finishedAt) {
            return new RetentionJobSnapshot(jobId, RetentionJobStatus.COMPLETED, generatedAlerts, startedAt, finishedAt, null);
        }

        RetentionJobSnapshot failed(String errorMessage, LocalDateTime finishedAt) {
            return new RetentionJobSnapshot(jobId, RetentionJobStatus.FAILED, generatedAlerts, startedAt, finishedAt, errorMessage);
        }
    }
}
