package com.gymiq.controller;

import com.gymiq.dto.request.CreateWorkoutSheetExerciseRequest;
import com.gymiq.dto.response.WorkoutSheetExerciseResponse;
import com.gymiq.entity.WorkoutBlock;
import com.gymiq.entity.WorkoutSheet;
import com.gymiq.entity.WorkoutSheetExercise;
import com.gymiq.service.WorkoutSheetExerciseService;
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

class WorkoutSheetExerciseControllerTest {

    @Test
    void addExerciseShouldReturnCreatedExercise() {
        WorkoutSheetExerciseService service = mock(WorkoutSheetExerciseService.class);
        WorkoutSheetExerciseController controller = new WorkoutSheetExerciseController(service);
        WorkoutSheet sheet = TestDataFactory.workoutSheet();
        WorkoutSheetExercise item = sheet.getBlocks().get(0).getExercises().get(0);
        CreateWorkoutSheetExerciseRequest request = request();
        Authentication authentication = authentication("admin@gymiq.com", "ROLE_ADMIN");

        when(service.addExercise(sheet.getWorkoutSheetId(), request, "admin@gymiq.com", true))
                .thenReturn(WorkoutSheetExerciseResponse.fromEntity(item));

        ResponseEntity<WorkoutSheetExerciseResponse> response = controller.addExercise(
                sheet.getWorkoutSheetId(),
                authentication,
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getExerciseName()).isEqualTo("Supino");
        verify(service).addExercise(sheet.getWorkoutSheetId(), request, "admin@gymiq.com", true);
    }

    @Test
    void findByWorkoutSheetShouldReturnPagedExercises() {
        WorkoutSheetExerciseService service = mock(WorkoutSheetExerciseService.class);
        WorkoutSheetExerciseController controller = new WorkoutSheetExerciseController(service);
        WorkoutSheet sheet = TestDataFactory.workoutSheet();
        WorkoutSheetExercise item = sheet.getBlocks().get(0).getExercises().get(0);
        Authentication authentication = authentication("ana@gymiq.com", "ROLE_STUDENT");

        when(service.findByWorkoutSheet(sheet.getWorkoutSheetId(), Pageable.unpaged(), "ana@gymiq.com", false, false, true))
                .thenReturn(new PageImpl<>(List.of(WorkoutSheetExerciseResponse.fromEntity(item))));

        ResponseEntity<Page<WorkoutSheetExerciseResponse>> response = controller.findByWorkoutSheet(
                sheet.getWorkoutSheetId(),
                authentication,
                Pageable.unpaged());

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        verify(service).findByWorkoutSheet(sheet.getWorkoutSheetId(), Pageable.unpaged(), "ana@gymiq.com", false, false, true);
    }

    @Test
    void addExerciseToBlockShouldReturnCreatedExercise() {
        WorkoutSheetExerciseService service = mock(WorkoutSheetExerciseService.class);
        WorkoutSheetExerciseController controller = new WorkoutSheetExerciseController(service);
        WorkoutBlock block = TestDataFactory.workoutSheet().getBlocks().get(0);
        WorkoutSheetExercise item = block.getExercises().get(0);
        CreateWorkoutSheetExerciseRequest request = request();
        Authentication authentication = authentication("carlos@gymiq.com", "ROLE_INSTRUCTOR");

        when(service.addExerciseToBlock(block.getWorkoutBlockId(), request, "carlos@gymiq.com", false))
                .thenReturn(WorkoutSheetExerciseResponse.fromEntity(item));

        ResponseEntity<WorkoutSheetExerciseResponse> response = controller.addExerciseToBlock(
                block.getWorkoutBlockId(),
                authentication,
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        verify(service).addExerciseToBlock(block.getWorkoutBlockId(), request, "carlos@gymiq.com", false);
    }

    @Test
    void findByWorkoutBlockShouldReturnPagedExercises() {
        WorkoutSheetExerciseService service = mock(WorkoutSheetExerciseService.class);
        WorkoutSheetExerciseController controller = new WorkoutSheetExerciseController(service);
        WorkoutBlock block = TestDataFactory.workoutSheet().getBlocks().get(0);
        WorkoutSheetExercise item = block.getExercises().get(0);
        Authentication authentication = authentication("admin@gymiq.com", "ROLE_ADMIN");

        when(service.findByWorkoutBlock(block.getWorkoutBlockId(), Pageable.unpaged(), "admin@gymiq.com", true, false, false))
                .thenReturn(new PageImpl<>(List.of(WorkoutSheetExerciseResponse.fromEntity(item))));

        ResponseEntity<Page<WorkoutSheetExerciseResponse>> response = controller.findByWorkoutBlock(
                block.getWorkoutBlockId(),
                authentication,
                Pageable.unpaged());

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTotalElements()).isEqualTo(1);
        verify(service).findByWorkoutBlock(block.getWorkoutBlockId(), Pageable.unpaged(), "admin@gymiq.com", true, false, false);
    }

    @Test
    void updateShouldReturnUpdatedExercise() {
        WorkoutSheetExerciseService service = mock(WorkoutSheetExerciseService.class);
        WorkoutSheetExerciseController controller = new WorkoutSheetExerciseController(service);
        WorkoutSheetExercise item = TestDataFactory.workoutSheet().getBlocks().get(0).getExercises().get(0);
        CreateWorkoutSheetExerciseRequest request = request();
        Authentication authentication = authentication("admin@gymiq.com", "ROLE_ADMIN");

        when(service.update(item.getWorkoutSheetExerciseId(), request, "admin@gymiq.com", true))
                .thenReturn(WorkoutSheetExerciseResponse.fromEntity(item));

        ResponseEntity<WorkoutSheetExerciseResponse> response = controller.update(
                item.getWorkoutSheetExerciseId(),
                authentication,
                request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getExerciseId()).isEqualTo(7);
        verify(service).update(item.getWorkoutSheetExerciseId(), request, "admin@gymiq.com", true);
    }

    @Test
    void deleteShouldReturnNoContent() {
        WorkoutSheetExerciseService service = mock(WorkoutSheetExerciseService.class);
        WorkoutSheetExerciseController controller = new WorkoutSheetExerciseController(service);
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000009");
        Authentication authentication = authentication("admin@gymiq.com", "ROLE_ADMIN");

        ResponseEntity<Void> response = controller.delete(id, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(service).delete(id, "admin@gymiq.com", true);
    }

    private CreateWorkoutSheetExerciseRequest request() {
        CreateWorkoutSheetExerciseRequest request = new CreateWorkoutSheetExerciseRequest();
        request.setExerciseId(7);
        request.setSets(4);
        request.setRepetitions("10");
        request.setRestSeconds(60);
        request.setExecutionOrder(1);
        request.setNotes("Controlar movimento");
        return request;
    }

    private Authentication authentication(String email, String role) {
        return new TestingAuthenticationToken(email, "n/a", role);
    }
}
