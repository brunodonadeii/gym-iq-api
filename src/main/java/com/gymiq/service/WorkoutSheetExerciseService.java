package com.gymiq.service;

import com.gymiq.dto.request.CreateWorkoutSheetExerciseRequest;
import com.gymiq.dto.response.WorkoutSheetExerciseResponse;
import com.gymiq.entity.Exercise;
import com.gymiq.entity.WorkoutSheet;
import com.gymiq.entity.WorkoutSheetExercise;
import com.gymiq.exception.BusinessException;
import com.gymiq.exception.ResourceNotFoundException;
import com.gymiq.repository.ExerciseRepository;
import com.gymiq.repository.WorkoutSheetExerciseRepository;
import com.gymiq.repository.WorkoutSheetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkoutSheetExerciseService {

    private final WorkoutSheetExerciseRepository workoutSheetExerciseRepository;
    private final WorkoutSheetRepository workoutSheetRepository;
    private final ExerciseRepository exerciseRepository;

    @Transactional
    public WorkoutSheetExerciseResponse addExercise(Integer workoutSheetId, CreateWorkoutSheetExerciseRequest request) {
        WorkoutSheet workoutSheet = findActiveWorkoutSheet(workoutSheetId);
        Exercise exercise = findExercise(request.getExerciseId());
        ensureOrderIsAvailable(workoutSheetId, request.getExecutionOrder(), null);

        WorkoutSheetExercise item = buildWorkoutSheetExercise(workoutSheet, exercise, request);
        workoutSheetExerciseRepository.save(item);

        log.info("Workout sheet exercise added: id={}, sheet={}",
                item.getWorkoutSheetExerciseId(), workoutSheetId);
        return WorkoutSheetExerciseResponse.fromEntity(item);
    }

    @Transactional(readOnly = true)
    public Page<WorkoutSheetExerciseResponse> findByWorkoutSheet(Integer workoutSheetId, Pageable pageable) {
        if (!workoutSheetRepository.existsById(workoutSheetId)) {
            throw new ResourceNotFoundException("Ficha de treino nao encontrada: " + workoutSheetId);
        }

        return workoutSheetExerciseRepository.findByWorkoutSheetWorkoutSheetId(workoutSheetId, pageable)
                .map(WorkoutSheetExerciseResponse::fromEntity);
    }

    @Transactional
    public WorkoutSheetExerciseResponse update(Integer id, CreateWorkoutSheetExerciseRequest request) {
        WorkoutSheetExercise item = findEntityById(id);
        Exercise exercise = findExercise(request.getExerciseId());
        Integer workoutSheetId = item.getWorkoutSheet().getWorkoutSheetId();

        ensureWorkoutSheetIsActive(item.getWorkoutSheet());
        ensureOrderIsAvailable(workoutSheetId, request.getExecutionOrder(), id);

        item.setExercise(exercise);
        item.setSets(request.getSets());
        item.setRepetitions(request.getRepetitions());
        item.setRestSeconds(request.getRestSeconds());
        item.setExecutionOrder(request.getExecutionOrder());
        item.setNotes(request.getNotes());

        workoutSheetExerciseRepository.save(item);
        log.info("Workout sheet exercise updated: id={}", id);
        return WorkoutSheetExerciseResponse.fromEntity(item);
    }

    @Transactional
    public void delete(Integer id) {
        WorkoutSheetExercise item = findEntityById(id);
        ensureWorkoutSheetIsActive(item.getWorkoutSheet());
        workoutSheetExerciseRepository.delete(item);
        log.info("Workout sheet exercise deleted: id={}", id);
    }

    private WorkoutSheetExercise findEntityById(Integer id) {
        return workoutSheetExerciseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item da ficha nao encontrado: " + id));
    }

    private WorkoutSheet findActiveWorkoutSheet(Integer workoutSheetId) {
        WorkoutSheet workoutSheet = workoutSheetRepository.findById(workoutSheetId)
                .orElseThrow(() -> new ResourceNotFoundException("Ficha de treino nao encontrada: " + workoutSheetId));

        ensureWorkoutSheetIsActive(workoutSheet);
        return workoutSheet;
    }

    private Exercise findExercise(Integer exerciseId) {
        return exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ResourceNotFoundException("Exercicio nao encontrado: " + exerciseId));
    }

    private void ensureWorkoutSheetIsActive(WorkoutSheet workoutSheet) {
        if (Boolean.FALSE.equals(workoutSheet.getActive())) {
            throw new BusinessException("Ficha de treino inativa nao pode ser alterada");
        }
    }

    private void ensureOrderIsAvailable(Integer workoutSheetId, Integer order, Integer currentItemId) {
        workoutSheetExerciseRepository.findByWorkoutSheetWorkoutSheetIdOrderByExecutionOrderAsc(workoutSheetId)
                .stream()
                .filter(item -> item.getExecutionOrder().equals(order))
                .filter(item -> !item.getWorkoutSheetExerciseId().equals(currentItemId))
                .findFirst()
                .ifPresent(item -> {
                    throw new BusinessException("Ordem de execucao ja usada nesta ficha: " + order);
                });
    }

    private WorkoutSheetExercise buildWorkoutSheetExercise(
            WorkoutSheet workoutSheet,
            Exercise exercise,
            CreateWorkoutSheetExerciseRequest request) {

        return WorkoutSheetExercise.builder()
                .workoutSheet(workoutSheet)
                .exercise(exercise)
                .sets(request.getSets())
                .repetitions(request.getRepetitions())
                .restSeconds(request.getRestSeconds())
                .executionOrder(request.getExecutionOrder())
                .notes(request.getNotes())
                .build();
    }
}
