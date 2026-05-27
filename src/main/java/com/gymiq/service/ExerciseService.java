package com.gymiq.service;

import com.gymiq.dto.request.CreateExerciseRequest;
import com.gymiq.dto.response.ExerciseResponse;
import com.gymiq.entity.Exercise;
import com.gymiq.exception.BusinessException;
import com.gymiq.exception.ResourceNotFoundException;
import com.gymiq.repository.ExerciseRepository;
import com.gymiq.repository.WorkoutSheetExerciseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final WorkoutSheetExerciseRepository workoutSheetExerciseRepository;

    @Transactional
    public ExerciseResponse create(CreateExerciseRequest request) {
        ensureNameIsAvailable(request.getName(), null);

        Exercise exercise = Exercise.builder()
                .name(request.getName())
                .muscleGroup(request.getMuscleGroup())
                .description(request.getDescription())
                .active(true)
                .build();

        exerciseRepository.save(exercise);
        log.info("Exercise created: id={}, name={}", exercise.getExerciseId(), exercise.getName());
        return ExerciseResponse.fromEntity(exercise);
    }

    @Transactional(readOnly = true)
    public Page<ExerciseResponse> findActive(Pageable pageable) {
        return exerciseRepository.findByActiveTrue(pageable)
                .map(ExerciseResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<ExerciseResponse> findAll(Pageable pageable) {
        return exerciseRepository.findAll(pageable)
                .map(ExerciseResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<ExerciseResponse> search(String term, Pageable pageable) {
        return exerciseRepository.searchByTerm(term, pageable)
                .map(ExerciseResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public ExerciseResponse findById(Integer id) {
        return ExerciseResponse.fromEntity(findEntityById(id));
    }

    @Transactional
    public ExerciseResponse update(Integer id, CreateExerciseRequest request) {
        Exercise exercise = findEntityById(id);
        ensureNameIsAvailable(request.getName(), id);

        exercise.setName(request.getName());
        exercise.setMuscleGroup(request.getMuscleGroup());
        exercise.setDescription(request.getDescription());

        exerciseRepository.save(exercise);
        log.info("Exercise updated: id={}", id);
        return ExerciseResponse.fromEntity(exercise);
    }

    @Transactional
    public void delete(Integer id) {
        Exercise exercise = findEntityById(id);

        if (workoutSheetExerciseRepository.existsByExerciseExerciseId(id)) {
            throw new BusinessException("Nao e possivel excluir um exercicio vinculado a fichas de treino");
        }

        exerciseRepository.delete(exercise);
        log.info("Exercise deleted: id={}", id);
    }

    public Exercise findEntityById(Integer id) {
        return exerciseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exercicio nao encontrado: " + id));
    }

    private void ensureNameIsAvailable(String name, Integer currentExerciseId) {
        exerciseRepository.findByNameIgnoreCase(name)
                .filter(exercise -> !exercise.getExerciseId().equals(currentExerciseId))
                .ifPresent(exercise -> {
                    throw new BusinessException("Ja existe um exercicio com o nome: " + name);
                });
    }
}
