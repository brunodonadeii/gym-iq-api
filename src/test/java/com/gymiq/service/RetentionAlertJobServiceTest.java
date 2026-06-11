package com.gymiq.service;

import com.gymiq.dto.response.RetentionAlertJobStatusResponse;
import com.gymiq.dto.response.RetentionAlertResponse;
import com.gymiq.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RetentionAlertJobServiceTest {

    @Test
    void getLatestJobStatusShouldStartIdle() {
        RetentionAlertService retentionAlertService = mock(RetentionAlertService.class);
        RetentionAlertJobService service = new RetentionAlertJobService(retentionAlertService, Runnable::run);

        RetentionAlertJobStatusResponse response = service.getLatestJobStatus();

        assertThat(response.getStatus()).isEqualTo("IDLE");
        assertThat(response.getGeneratedAlerts()).isZero();
        assertThat(response.getMessage()).contains("Nenhuma");
    }

    @Test
    void startGenerateActiveStudentsJobShouldCompleteWhenExecutorRunsSynchronously() {
        RetentionAlertService retentionAlertService = mock(RetentionAlertService.class);
        when(retentionAlertService.generateForActiveStudents())
                .thenReturn(List.of(
                        RetentionAlertResponse.fromEntity(TestDataFactory.openRetentionAlert()),
                        RetentionAlertResponse.fromEntity(TestDataFactory.openRetentionAlert())));
        RetentionAlertJobService service = new RetentionAlertJobService(retentionAlertService, Runnable::run);

        RetentionAlertJobStatusResponse startResponse = service.startGenerateActiveStudentsJob();
        RetentionAlertJobStatusResponse latestResponse = service.getLatestJobStatus();

        assertThat(startResponse.getStatus()).isEqualTo("RUNNING");
        assertThat(latestResponse.getStatus()).isEqualTo("COMPLETED");
        assertThat(latestResponse.getGeneratedAlerts()).isEqualTo(2);
        assertThat(latestResponse.getFinishedAt()).isNotNull();
    }

    @Test
    void startGenerateActiveStudentsJobShouldRejectConcurrentRun() {
        RetentionAlertService retentionAlertService = mock(RetentionAlertService.class);
        Executor pausedExecutor = command -> { };
        RetentionAlertJobService service = new RetentionAlertJobService(retentionAlertService, pausedExecutor);

        RetentionAlertJobStatusResponse first = service.startGenerateActiveStudentsJob();
        RetentionAlertJobStatusResponse second = service.startGenerateActiveStudentsJob();

        assertThat(first.getStatus()).isEqualTo("RUNNING");
        assertThat(second.getStatus()).isEqualTo("RUNNING");
        assertThat(second.getMessage()).contains("ja esta em processamento");
    }

    @Test
    void startGenerateActiveStudentsJobShouldStoreFailureStatus() {
        RetentionAlertService retentionAlertService = mock(RetentionAlertService.class);
        when(retentionAlertService.generateForActiveStudents()).thenThrow(new RuntimeException("falha externa"));
        RetentionAlertJobService service = new RetentionAlertJobService(retentionAlertService, Runnable::run);

        service.startGenerateActiveStudentsJob();
        RetentionAlertJobStatusResponse latestResponse = service.getLatestJobStatus();

        assertThat(latestResponse.getStatus()).isEqualTo("FAILED");
        assertThat(latestResponse.getErrorMessage()).isEqualTo("falha externa");
        assertThat(latestResponse.getMessage()).contains("falhou");
    }
}
