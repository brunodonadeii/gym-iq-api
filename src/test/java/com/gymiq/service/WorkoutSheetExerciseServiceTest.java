package com.gymiq.service;

import com.gymiq.dto.request.CreateWorkoutSheetExerciseRequest;
import com.gymiq.dto.response.WorkoutSheetExerciseResponse;
import com.gymiq.entity.WorkoutBlock;
import com.gymiq.entity.WorkoutSheet;
import com.gymiq.entity.WorkoutSheetExercise;
import com.gymiq.exception.BusinessException;
import com.gymiq.repository.ExerciseRepository;
import com.gymiq.repository.WorkoutBlockRepository;
import com.gymiq.repository.WorkoutSheetExerciseRepository;
import com.gymiq.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutSheetExerciseServiceTest {

    @Mock
    private WorkoutSheetExerciseRepository workoutSheetExerciseRepository;

    @Mock
    private WorkoutBlockRepository workoutBlockRepository;

    @Mock
    private WorkoutSheetService workoutSheetService;

    @Mock
    private ExerciseRepository exerciseRepository;

    @InjectMocks
    private WorkoutSheetExerciseService workoutSheetExerciseService;

    @Test
    void addExerciseShouldPersistItemWhenOrderIsAvailable() {
        WorkoutSheet workoutSheet = TestDataFactory.workoutSheet();
        WorkoutBlock block = workoutSheet.getBlocks().get(0);
        CreateWorkoutSheetExerciseRequest request = exerciseItem("Treino A", 2);

        when(workoutBlockRepository.findById(block.getWorkoutBlockId())).thenReturn(Optional.of(block));
        when(exerciseRepository.findById(request.getExerciseId())).thenReturn(Optional.of(TestDataFactory.exercise()));
        when(workoutSheetExerciseRepository.findByWorkoutBlockWorkoutBlockId(block.getWorkoutBlockId(), Pageable.unpaged()))
                .thenReturn(Page.empty());
        when(workoutSheetExerciseRepository.save(any(WorkoutSheetExercise.class))).thenAnswer(invocation -> {
            WorkoutSheetExercise item = invocation.getArgument(0);
            item.setWorkoutSheetExerciseId(UUID.fromString("00000000-0000-0000-0000-000000000099"));
            return item;
        });

        WorkoutSheetExerciseResponse response = workoutSheetExerciseService.addExerciseToBlock(block.getWorkoutBlockId(), request);

        assertThat(response.getWorkoutSheetExerciseId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000099"));
        assertThat(response.getBlockName()).isEqualTo("Treino A");
        verify(workoutSheetExerciseRepository).save(any(WorkoutSheetExercise.class));
    }

    @Test
    void updateShouldRejectDuplicatedOrderInSameTrainingSection() {
        WorkoutSheet workoutSheet = TestDataFactory.workoutSheet();
        WorkoutBlock block = workoutSheet.getBlocks().get(0);
        WorkoutSheetExercise current = block.getExercises().get(0);
        WorkoutSheetExercise duplicated = TestDataFactory.workoutSheetExercise(block);
        duplicated.setWorkoutSheetExerciseId(UUID.fromString("00000000-0000-0000-0000-000000000100"));
        CreateWorkoutSheetExerciseRequest request = exerciseItem("Treino A", 1);

        when(workoutSheetExerciseRepository.findById(current.getWorkoutSheetExerciseId())).thenReturn(Optional.of(current));
        when(exerciseRepository.findById(request.getExerciseId())).thenReturn(Optional.of(TestDataFactory.exercise()));
        when(workoutSheetExerciseRepository.findByWorkoutBlockWorkoutBlockId(block.getWorkoutBlockId(), Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(current, duplicated)));

        assertThatThrownBy(() -> workoutSheetExerciseService.update(current.getWorkoutSheetExerciseId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("usada");
    }

    @Test
    void deleteShouldRejectInstructorWithoutOwnership() {
        WorkoutSheet workoutSheet = TestDataFactory.workoutSheet();
        WorkoutSheetExercise item = workoutSheet.getBlocks().get(0).getExercises().get(0);

        when(workoutSheetExerciseRepository.findById(item.getWorkoutSheetExerciseId())).thenReturn(Optional.of(item));
        doThrow(new AccessDeniedException("Instrutor não tem permissão para alterar esta ficha"))
                .when(workoutSheetService)
                .ensureInstructorCanManage(workoutSheet, "outro@gymiq.com", false);

        assertThatThrownBy(() -> workoutSheetExerciseService.delete(item.getWorkoutSheetExerciseId(), "outro@gymiq.com", false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteShouldRemoveItemForAdmin() {
        WorkoutSheet workoutSheet = TestDataFactory.workoutSheet();
        WorkoutSheetExercise item = workoutSheet.getBlocks().get(0).getExercises().get(0);

        when(workoutSheetExerciseRepository.findById(item.getWorkoutSheetExerciseId())).thenReturn(Optional.of(item));

        workoutSheetExerciseService.delete(item.getWorkoutSheetExerciseId());

        verify(workoutSheetExerciseRepository).delete(item);
    }

    private CreateWorkoutSheetExerciseRequest exerciseItem(String section, int order) {
        CreateWorkoutSheetExerciseRequest request = new CreateWorkoutSheetExerciseRequest();
        request.setExerciseId(7);
        request.setSets(4);
        request.setRepetitions("10");
        request.setRestSeconds(60);
        request.setTrainingSection(section);
        request.setExecutionOrder(order);
        request.setNotes("Controlar movimento");
        return request;
    }
}
