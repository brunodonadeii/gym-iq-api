package com.gymiq.service;

import com.gymiq.dto.request.CreateWorkoutBlockRequest;
import com.gymiq.dto.request.CreateWorkoutSheetExerciseRequest;
import com.gymiq.dto.response.WorkoutBlockResponse;
import com.gymiq.dto.response.WorkoutBlockSummaryResponse;
import com.gymiq.entity.WorkoutBlock;
import com.gymiq.entity.WorkoutSheet;
import com.gymiq.exception.BusinessException;
import com.gymiq.exception.ResourceNotFoundException;
import com.gymiq.repository.ExerciseRepository;
import com.gymiq.repository.WorkoutBlockRepository;
import com.gymiq.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutBlockServiceTest {

    @Mock
    private WorkoutBlockRepository workoutBlockRepository;

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private WorkoutSheetService workoutSheetService;

    @InjectMocks
    private WorkoutBlockService workoutBlockService;

    @Test
    void createShouldPersistBlockWhenNameAndOrderAreAvailable() {
        WorkoutSheet workoutSheet = TestDataFactory.workoutSheet();
        CreateWorkoutBlockRequest request = validRequest("Treino B", 2);

        when(workoutSheetService.findEntityById(workoutSheet.getWorkoutSheetId())).thenReturn(workoutSheet);
        when(workoutBlockRepository.findByWorkoutSheetWorkoutSheetIdAndNameIgnoreCase(workoutSheet.getWorkoutSheetId(), "Treino B"))
                .thenReturn(Optional.empty());
        when(workoutBlockRepository.findByWorkoutSheetWorkoutSheetIdOrderByExecutionOrderAsc(workoutSheet.getWorkoutSheetId()))
                .thenReturn(List.of(workoutSheet.getBlocks().get(0)));
        when(workoutBlockRepository.save(any(WorkoutBlock.class))).thenAnswer(invocation -> {
            WorkoutBlock block = invocation.getArgument(0);
            block.setWorkoutBlockId(UUID.fromString("00000000-0000-0000-0000-000000000200"));
            return block;
        });

        WorkoutBlockResponse response = workoutBlockService.create(
                workoutSheet.getWorkoutSheetId(),
                request,
                "carlos@gymiq.com",
                false);

        assertThat(response.getWorkoutBlockId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000200"));
        assertThat(response.getName()).isEqualTo("Treino B");
        verify(workoutSheetService).ensureWorkoutSheetIsActive(workoutSheet);
        verify(workoutSheetService).ensureInstructorCanManage(workoutSheet, "carlos@gymiq.com", false);
        verify(workoutBlockRepository).save(any(WorkoutBlock.class));
    }

    @Test
    void createShouldRejectDuplicatedName() {
        WorkoutSheet workoutSheet = TestDataFactory.workoutSheet();
        WorkoutBlock existing = workoutSheet.getBlocks().get(0);
        CreateWorkoutBlockRequest request = validRequest("Treino A", 2);

        when(workoutSheetService.findEntityById(workoutSheet.getWorkoutSheetId())).thenReturn(workoutSheet);
        when(workoutBlockRepository.findByWorkoutSheetWorkoutSheetIdAndNameIgnoreCase(workoutSheet.getWorkoutSheetId(), "Treino A"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> workoutBlockService.create(workoutSheet.getWorkoutSheetId(), request, "carlos@gymiq.com", false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("nome");
    }

    @Test
    void createShouldRejectDuplicatedOrder() {
        WorkoutSheet workoutSheet = TestDataFactory.workoutSheet();
        CreateWorkoutBlockRequest request = validRequest("Treino B", 1);

        when(workoutSheetService.findEntityById(workoutSheet.getWorkoutSheetId())).thenReturn(workoutSheet);
        when(workoutBlockRepository.findByWorkoutSheetWorkoutSheetIdAndNameIgnoreCase(workoutSheet.getWorkoutSheetId(), "Treino B"))
                .thenReturn(Optional.empty());
        when(workoutBlockRepository.findByWorkoutSheetWorkoutSheetIdOrderByExecutionOrderAsc(workoutSheet.getWorkoutSheetId()))
                .thenReturn(workoutSheet.getBlocks());

        assertThatThrownBy(() -> workoutBlockService.create(workoutSheet.getWorkoutSheetId(), request, "carlos@gymiq.com", false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ordem");
    }

    @Test
    void createShouldRejectDuplicatedExerciseOrderInsideBlock() {
        WorkoutSheet workoutSheet = TestDataFactory.workoutSheet();
        CreateWorkoutBlockRequest request = validRequest("Treino B", 2);
        request.setExercises(List.of(exerciseItem(1), exerciseItem(1)));

        when(workoutSheetService.findEntityById(workoutSheet.getWorkoutSheetId())).thenReturn(workoutSheet);
        when(workoutBlockRepository.findByWorkoutSheetWorkoutSheetIdAndNameIgnoreCase(workoutSheet.getWorkoutSheetId(), "Treino B"))
                .thenReturn(Optional.empty());
        when(workoutBlockRepository.findByWorkoutSheetWorkoutSheetIdOrderByExecutionOrderAsc(workoutSheet.getWorkoutSheetId()))
                .thenReturn(workoutSheet.getBlocks());
        when(exerciseRepository.findById(7)).thenReturn(Optional.of(TestDataFactory.exercise()));

        assertThatThrownBy(() -> workoutBlockService.create(workoutSheet.getWorkoutSheetId(), request, "carlos@gymiq.com", false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("duplicada");
    }

    @Test
    void findByWorkoutSheetShouldReturnSummaryPage() {
        WorkoutSheet workoutSheet = TestDataFactory.workoutSheet();

        when(workoutSheetService.findEntityById(workoutSheet.getWorkoutSheetId())).thenReturn(workoutSheet);
        when(workoutBlockRepository.findByWorkoutSheetWorkoutSheetId(workoutSheet.getWorkoutSheetId(), Pageable.unpaged()))
                .thenReturn(new PageImpl<>(workoutSheet.getBlocks()));

        Page<WorkoutBlockSummaryResponse> response = workoutBlockService.findByWorkoutSheet(
                workoutSheet.getWorkoutSheetId(),
                Pageable.unpaged(),
                "aluno@gymiq.com",
                false,
                false,
                true);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getName()).isEqualTo("Treino A");
        verify(workoutSheetService).ensureCanViewWorkoutSheet(workoutSheet, "aluno@gymiq.com", false, false, true);
    }

    @Test
    void updateShouldChangeBlockData() {
        WorkoutSheet workoutSheet = TestDataFactory.workoutSheet();
        WorkoutBlock block = workoutSheet.getBlocks().get(0);
        CreateWorkoutBlockRequest request = validRequest("Treino Superior", 3);

        when(workoutBlockRepository.findById(block.getWorkoutBlockId())).thenReturn(Optional.of(block));
        when(workoutBlockRepository.findByWorkoutSheetWorkoutSheetIdAndNameIgnoreCase(workoutSheet.getWorkoutSheetId(), "Treino Superior"))
                .thenReturn(Optional.empty());
        when(workoutBlockRepository.findByWorkoutSheetWorkoutSheetIdOrderByExecutionOrderAsc(workoutSheet.getWorkoutSheetId()))
                .thenReturn(List.of(block));

        WorkoutBlockResponse response = workoutBlockService.update(block.getWorkoutBlockId(), request, "carlos@gymiq.com", false);

        assertThat(response.getName()).isEqualTo("Treino Superior");
        assertThat(response.getExecutionOrder()).isEqualTo(3);
        verify(workoutBlockRepository).save(block);
    }

    @Test
    void deleteShouldRemoveBlock() {
        WorkoutSheet workoutSheet = TestDataFactory.workoutSheet();
        WorkoutBlock block = workoutSheet.getBlocks().get(0);

        when(workoutBlockRepository.findById(block.getWorkoutBlockId())).thenReturn(Optional.of(block));

        workoutBlockService.delete(block.getWorkoutBlockId(), "carlos@gymiq.com", false);

        verify(workoutBlockRepository).delete(block);
    }

    @Test
    void findByIdShouldThrowWhenBlockDoesNotExist() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000404");

        when(workoutBlockRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workoutBlockService.findById(id, "admin@gymiq.com", true, false, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private CreateWorkoutBlockRequest validRequest(String name, int order) {
        CreateWorkoutBlockRequest request = new CreateWorkoutBlockRequest();
        request.setName(name);
        request.setDescription("Descricao do treino");
        request.setExecutionOrder(order);
        return request;
    }

    private CreateWorkoutSheetExerciseRequest exerciseItem(int order) {
        CreateWorkoutSheetExerciseRequest request = new CreateWorkoutSheetExerciseRequest();
        request.setExerciseId(7);
        request.setSets(4);
        request.setRepetitions("10");
        request.setRestSeconds(60);
        request.setExecutionOrder(order);
        return request;
    }
}
