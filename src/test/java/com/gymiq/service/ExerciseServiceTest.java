package com.gymiq.service;

import com.gymiq.dto.request.CreateExerciseRequest;
import com.gymiq.dto.response.ExerciseResponse;
import com.gymiq.entity.Exercise;
import com.gymiq.exception.BusinessException;
import com.gymiq.repository.ExerciseRepository;
import com.gymiq.repository.WorkoutSheetExerciseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExerciseServiceTest {

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private WorkoutSheetExerciseRepository workoutSheetExerciseRepository;

    @InjectMocks
    private ExerciseService exerciseService;

    @Test
    void createShouldPersistExercise() {
        CreateExerciseRequest request = exerciseRequest("Supino");

        when(exerciseRepository.findByNameIgnoreCase(request.getName())).thenReturn(Optional.empty());
        when(exerciseRepository.save(any(Exercise.class))).thenAnswer(invocation -> {
            Exercise exercise = invocation.getArgument(0);
            exercise.setExerciseId(7);
            return exercise;
        });

        ExerciseResponse response = exerciseService.create(request);

        assertThat(response.getExerciseId()).isEqualTo(7);
        assertThat(response.getName()).isEqualTo("Supino");
    }

    @Test
    void createShouldRejectDuplicatedName() {
        CreateExerciseRequest request = exerciseRequest("Supino");
        Exercise existing = Exercise.builder()
                .name("Supino")
                .muscleGroup("Peito")
                .build();
        existing.setExerciseId(7);

        when(exerciseRepository.findByNameIgnoreCase(request.getName())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> exerciseService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("nome");
    }

    @Test
    void deleteShouldRemoveExerciseWhenNotLinkedToWorkoutSheet() {
        Exercise exercise = Exercise.builder()
                .name("Remada")
                .muscleGroup("Costas")
                .build();
        exercise.setExerciseId(9);

        when(exerciseRepository.findById(exercise.getExerciseId())).thenReturn(Optional.of(exercise));
        when(workoutSheetExerciseRepository.existsByExerciseExerciseId(exercise.getExerciseId())).thenReturn(false);

        exerciseService.delete(exercise.getExerciseId());

        verify(exerciseRepository).delete(exercise);
    }

    @Test
    void deleteShouldRejectExerciseLinkedToWorkoutSheet() {
        Exercise exercise = Exercise.builder()
                .name("Remada")
                .muscleGroup("Costas")
                .build();
        exercise.setExerciseId(9);

        when(exerciseRepository.findById(exercise.getExerciseId())).thenReturn(Optional.of(exercise));
        when(workoutSheetExerciseRepository.existsByExerciseExerciseId(exercise.getExerciseId())).thenReturn(true);

        assertThatThrownBy(() -> exerciseService.delete(exercise.getExerciseId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("fichas");
    }

    private CreateExerciseRequest exerciseRequest(String name) {
        CreateExerciseRequest request = new CreateExerciseRequest();
        request.setName(name);
        request.setMuscleGroup("Peito");
        request.setDescription("Exercicio de forca");
        return request;
    }
}
