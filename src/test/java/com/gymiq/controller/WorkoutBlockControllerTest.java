package com.gymiq.controller;

import com.gymiq.dto.request.CreateWorkoutBlockRequest;
import com.gymiq.dto.response.WorkoutBlockResponse;
import com.gymiq.dto.response.WorkoutBlockSummaryResponse;
import com.gymiq.entity.WorkoutBlock;
import com.gymiq.entity.WorkoutSheet;
import com.gymiq.service.WorkoutBlockService;
import com.gymiq.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkoutBlockControllerTest {

    @Test
    void createShouldReturnCreatedBlock() {
        WorkoutBlockService service = mock(WorkoutBlockService.class);
        WorkoutBlockController controller = new WorkoutBlockController(service);
        WorkoutSheet sheet = TestDataFactory.workoutSheet();
        WorkoutBlock block = sheet.getBlocks().get(0);
        CreateWorkoutBlockRequest request = request();
        Authentication authentication = authentication("admin@gymiq.com", "ROLE_ADMIN");

        when(service.create(sheet.getWorkoutSheetId(), request, "admin@gymiq.com", true))
                .thenReturn(WorkoutBlockResponse.fromEntity(block));

        ResponseEntity<WorkoutBlockResponse> response = controller.create(sheet.getWorkoutSheetId(), authentication, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Treino A");
        verify(service).create(sheet.getWorkoutSheetId(), request, "admin@gymiq.com", true);
    }

    @Test
    void findByWorkoutSheetShouldReturnBlocksForStudent() {
        WorkoutBlockService service = mock(WorkoutBlockService.class);
        WorkoutBlockController controller = new WorkoutBlockController(service);
        WorkoutSheet sheet = TestDataFactory.workoutSheet();
        Authentication authentication = authentication("ana@gymiq.com", "ROLE_STUDENT");

        when(service.findByWorkoutSheet(sheet.getWorkoutSheetId(), Pageable.unpaged(), "ana@gymiq.com", false, false, true))
                .thenReturn(new PageImpl<>(List.of(WorkoutBlockSummaryResponse.fromEntity(sheet.getBlocks().get(0)))));

        ResponseEntity<Page<WorkoutBlockSummaryResponse>> response = controller.findByWorkoutSheet(
                sheet.getWorkoutSheetId(),
                authentication,
                Pageable.unpaged());

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        verify(service).findByWorkoutSheet(sheet.getWorkoutSheetId(), Pageable.unpaged(), "ana@gymiq.com", false, false, true);
    }

    @Test
    void findByIdShouldReturnBlockDetailForInstructor() {
        WorkoutBlockService service = mock(WorkoutBlockService.class);
        WorkoutBlockController controller = new WorkoutBlockController(service);
        WorkoutBlock block = TestDataFactory.workoutSheet().getBlocks().get(0);
        Authentication authentication = authentication("carlos@gymiq.com", "ROLE_INSTRUCTOR");

        when(service.findById(block.getWorkoutBlockId(), "carlos@gymiq.com", false, true, false))
                .thenReturn(WorkoutBlockResponse.fromEntity(block));

        ResponseEntity<WorkoutBlockResponse> response = controller.findById(block.getWorkoutBlockId(), authentication);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getWorkoutBlockId()).isEqualTo(block.getWorkoutBlockId());
        verify(service).findById(block.getWorkoutBlockId(), "carlos@gymiq.com", false, true, false);
    }

    @Test
    void updateShouldReturnUpdatedBlock() {
        WorkoutBlockService service = mock(WorkoutBlockService.class);
        WorkoutBlockController controller = new WorkoutBlockController(service);
        WorkoutBlock block = TestDataFactory.workoutSheet().getBlocks().get(0);
        CreateWorkoutBlockRequest request = request();
        Authentication authentication = authentication("admin@gymiq.com", "ROLE_ADMIN");

        when(service.update(block.getWorkoutBlockId(), request, "admin@gymiq.com", true))
                .thenReturn(WorkoutBlockResponse.fromEntity(block));

        ResponseEntity<WorkoutBlockResponse> response = controller.update(block.getWorkoutBlockId(), authentication, request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Treino A");
        verify(service).update(block.getWorkoutBlockId(), request, "admin@gymiq.com", true);
    }

    @Test
    void deleteShouldReturnNoContent() {
        WorkoutBlockService service = mock(WorkoutBlockService.class);
        WorkoutBlockController controller = new WorkoutBlockController(service);
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000088");
        Authentication authentication = authentication("admin@gymiq.com", "ROLE_ADMIN");

        ResponseEntity<Void> response = controller.delete(id, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(service).delete(id, "admin@gymiq.com", true);
    }

    private CreateWorkoutBlockRequest request() {
        CreateWorkoutBlockRequest request = new CreateWorkoutBlockRequest();
        request.setName("Treino A");
        request.setDescription("Peito e triceps");
        request.setExecutionOrder(1);
        return request;
    }

    private Authentication authentication(String email, String role) {
        return new TestingAuthenticationToken(email, "n/a", role);
    }
}
