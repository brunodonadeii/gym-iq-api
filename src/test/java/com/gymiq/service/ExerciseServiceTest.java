package com.gymiq.service;

import com.gymiq.dto.request.CreateExerciseRequest;
import com.gymiq.dto.response.ExerciseResponse;
import com.gymiq.entity.Exercise;
import com.gymiq.exception.BusinessException;
import com.gymiq.repository.ExerciseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
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

    @InjectMocks
    private ExerciseService exerciseService;

    @Test
    void createShouldPersistActiveExercise() {
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
        assertThat(response.getActive()).isTrue();
    }

    @Test
    void createShouldRejectDuplicatedName() {
        CreateExerciseRequest request = exerciseRequest("Supino");
        Exercise existing = Exercise.builder()
                .name("Supino")
                .muscleGroup("Peito")
                .active(true)
                .build();
        existing.setExerciseId(7);

        when(exerciseRepository.findByNameIgnoreCase(request.getName())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> exerciseService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exercicio");
    }

    @Test
    void findActiveShouldMapRepositoryResult() {
        Exercise exercise = Exercise.builder()
                .name("Agachamento")
                .muscleGroup("Pernas")
                .active(true)
                .build();
        exercise.setExerciseId(8);

        when(exerciseRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(exercise));

        List<ExerciseResponse> responses = exerciseService.findActive();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("Agachamento");
    }

    @Test
    void deactivateShouldMarkExerciseAsInactive() {
        Exercise exercise = Exercise.builder()
                .name("Remada")
                .muscleGroup("Costas")
                .active(true)
                .build();
        exercise.setExerciseId(9);

        when(exerciseRepository.findById(exercise.getExerciseId())).thenReturn(Optional.of(exercise));

        exerciseService.deactivate(exercise.getExerciseId());

        assertThat(exercise.getActive()).isFalse();
        verify(exerciseRepository).save(exercise);
    }

    private CreateExerciseRequest exerciseRequest(String name) {
        CreateExerciseRequest request = new CreateExerciseRequest();
        request.setName(name);
        request.setMuscleGroup("Peito");
        request.setDescription("Exercicio de forca");
        return request;
    }
}
