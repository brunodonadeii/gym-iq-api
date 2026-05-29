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
import org.springframework.security.access.AccessDeniedException;
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
        return addExercise(workoutSheetId, request, null, true);
    }

    @Transactional
    public WorkoutSheetExerciseResponse addExercise(
            Integer workoutSheetId,
            CreateWorkoutSheetExerciseRequest request,
            String authenticatedEmail,
            boolean admin) {
        WorkoutSheet workoutSheet = findActiveWorkoutSheet(workoutSheetId);
        ensureInstructorCanManage(workoutSheet, authenticatedEmail, admin);
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
        findWorkoutSheet(workoutSheetId);

        return workoutSheetExerciseRepository.findByWorkoutSheetWorkoutSheetId(workoutSheetId, pageable)
                .map(WorkoutSheetExerciseResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<WorkoutSheetExerciseResponse> findByWorkoutSheet(
            Integer workoutSheetId,
            Pageable pageable,
            String authenticatedEmail,
            boolean admin,
            boolean instructor,
            boolean student) {
        WorkoutSheet workoutSheet = findWorkoutSheet(workoutSheetId);
        ensureCanViewWorkoutSheet(workoutSheet, authenticatedEmail, admin, instructor, student);

        return workoutSheetExerciseRepository.findByWorkoutSheetWorkoutSheetId(workoutSheetId, pageable)
                .map(WorkoutSheetExerciseResponse::fromEntity);
    }

    @Transactional
    public WorkoutSheetExerciseResponse update(Integer id, CreateWorkoutSheetExerciseRequest request) {
        return update(id, request, null, true);
    }

    @Transactional
    public WorkoutSheetExerciseResponse update(
            Integer id,
            CreateWorkoutSheetExerciseRequest request,
            String authenticatedEmail,
            boolean admin) {
        WorkoutSheetExercise item = findEntityById(id);
        Exercise exercise = findExercise(request.getExerciseId());
        Integer workoutSheetId = item.getWorkoutSheet().getWorkoutSheetId();

        ensureWorkoutSheetIsActive(item.getWorkoutSheet());
        ensureInstructorCanManage(item.getWorkoutSheet(), authenticatedEmail, admin);
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
        delete(id, null, true);
    }

    @Transactional
    public void delete(Integer id, String authenticatedEmail, boolean admin) {
        WorkoutSheetExercise item = findEntityById(id);
        ensureWorkoutSheetIsActive(item.getWorkoutSheet());
        ensureInstructorCanManage(item.getWorkoutSheet(), authenticatedEmail, admin);
        workoutSheetExerciseRepository.delete(item);
        log.info("Workout sheet exercise deleted: id={}", id);
    }

    private WorkoutSheetExercise findEntityById(Integer id) {
        return workoutSheetExerciseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item da ficha nao encontrado: " + id));
    }

    private WorkoutSheet findActiveWorkoutSheet(Integer workoutSheetId) {
        WorkoutSheet workoutSheet = findWorkoutSheet(workoutSheetId);

        ensureWorkoutSheetIsActive(workoutSheet);
        return workoutSheet;
    }

    private WorkoutSheet findWorkoutSheet(Integer workoutSheetId) {
        return workoutSheetRepository.findById(workoutSheetId)
                .orElseThrow(() -> new ResourceNotFoundException("Ficha de treino nao encontrada: " + workoutSheetId));
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

    private void ensureCanViewWorkoutSheet(
            WorkoutSheet workoutSheet,
            String authenticatedEmail,
            boolean admin,
            boolean instructor,
            boolean student) {
        if (admin) {
            return;
        }

        if (instructor && workoutSheet.getInstructor().getUser().getEmail().equalsIgnoreCase(authenticatedEmail)) {
            return;
        }

        if (student && workoutSheet.getStudent().getUser().getEmail().equalsIgnoreCase(authenticatedEmail)) {
            return;
        }

        throw new AccessDeniedException("Usuario nao tem permissao para acessar esta ficha");
    }

    private void ensureInstructorCanManage(WorkoutSheet workoutSheet, String authenticatedEmail, boolean admin) {
        if (admin) {
            return;
        }

        if (authenticatedEmail == null
                || !workoutSheet.getInstructor().getUser().getEmail().equalsIgnoreCase(authenticatedEmail)) {
            throw new AccessDeniedException("Instrutor nao tem permissao para alterar esta ficha");
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
