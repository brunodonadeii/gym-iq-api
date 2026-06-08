package com.gymiq.service;

import com.gymiq.aop.Auditable;
import com.gymiq.dto.request.CreateWorkoutSheetExerciseRequest;
import com.gymiq.dto.response.WorkoutSheetExerciseResponse;
import com.gymiq.entity.Exercise;
import com.gymiq.entity.WorkoutSheet;
import com.gymiq.entity.WorkoutSheetExercise;
import com.gymiq.enums.AuditAction;
import com.gymiq.enums.ResourceType;
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
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkoutSheetExerciseService {

    private static final String DEFAULT_TRAINING_SECTION = "A";

    private final WorkoutSheetExerciseRepository workoutSheetExerciseRepository;
    private final WorkoutSheetRepository workoutSheetRepository;
    private final ExerciseRepository exerciseRepository;

    @Transactional
    public WorkoutSheetExerciseResponse addExercise(UUID workoutSheetId, CreateWorkoutSheetExerciseRequest request) {
        return addExercise(workoutSheetId, request, null, true);
    }

    @Transactional
    @Auditable(action = AuditAction.ADD_WORKOUT_SHEET_EXERCISE, resourceType = ResourceType.WORKOUT_SHEET_EXERCISE, description = "Adicionou exercicio na ficha")
    public WorkoutSheetExerciseResponse addExercise(
            UUID workoutSheetId,
            CreateWorkoutSheetExerciseRequest request,
            String authenticatedEmail,
            boolean admin) {
        WorkoutSheet workoutSheet = findActiveWorkoutSheet(workoutSheetId);
        ensureInstructorCanManage(workoutSheet, authenticatedEmail, admin);
        Exercise exercise = findExercise(request.getExerciseId());
        ensureOrderIsAvailable(workoutSheetId, resolveTrainingSection(request.getTrainingSection()), request.getExecutionOrder(), null);

        WorkoutSheetExercise item = buildWorkoutSheetExercise(workoutSheet, exercise, request);
        workoutSheetExerciseRepository.save(item);

        log.info("Workout sheet exercise added: id={}, sheet={}",
                item.getWorkoutSheetExerciseId(), workoutSheetId);
        return WorkoutSheetExerciseResponse.fromEntity(item);
    }

    @Transactional(readOnly = true)
    public Page<WorkoutSheetExerciseResponse> findByWorkoutSheet(UUID workoutSheetId, Pageable pageable) {
        findWorkoutSheet(workoutSheetId);

        return workoutSheetExerciseRepository.findByWorkoutSheetWorkoutSheetId(workoutSheetId, pageable)
                .map(WorkoutSheetExerciseResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<WorkoutSheetExerciseResponse> findByWorkoutSheet(
            UUID workoutSheetId,
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
    public WorkoutSheetExerciseResponse update(UUID id, CreateWorkoutSheetExerciseRequest request) {
        return update(id, request, null, true);
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE_WORKOUT_SHEET_EXERCISE, resourceType = ResourceType.WORKOUT_SHEET_EXERCISE, description = "Atualizou exercicio da ficha")
    public WorkoutSheetExerciseResponse update(
            UUID id,
            CreateWorkoutSheetExerciseRequest request,
            String authenticatedEmail,
            boolean admin) {
        WorkoutSheetExercise item = findEntityById(id);
        Exercise exercise = findExercise(request.getExerciseId());
        UUID workoutSheetId = item.getWorkoutSheet().getWorkoutSheetId();

        ensureWorkoutSheetIsActive(item.getWorkoutSheet());
        ensureInstructorCanManage(item.getWorkoutSheet(), authenticatedEmail, admin);
        ensureOrderIsAvailable(workoutSheetId, resolveTrainingSection(request.getTrainingSection()), request.getExecutionOrder(), id);

        item.setExercise(exercise);
        item.setSets(request.getSets());
        item.setRepetitions(request.getRepetitions());
        item.setRestSeconds(request.getRestSeconds());
        item.setTrainingSection(resolveTrainingSection(request.getTrainingSection()));
        item.setExecutionOrder(request.getExecutionOrder());
        item.setNotes(request.getNotes());

        workoutSheetExerciseRepository.save(item);
        log.info("Workout sheet exercise updated: id={}", id);
        return WorkoutSheetExerciseResponse.fromEntity(item);
    }

    @Transactional
    public void delete(UUID id) {
        delete(id, null, true);
    }

    @Transactional
    @Auditable(action = AuditAction.DELETE_WORKOUT_SHEET_EXERCISE, resourceType = ResourceType.WORKOUT_SHEET_EXERCISE, description = "Removeu exercicio da ficha")
    public void delete(UUID id, String authenticatedEmail, boolean admin) {
        WorkoutSheetExercise item = findEntityById(id);
        ensureWorkoutSheetIsActive(item.getWorkoutSheet());
        ensureInstructorCanManage(item.getWorkoutSheet(), authenticatedEmail, admin);
        workoutSheetExerciseRepository.delete(item);
        log.info("Workout sheet exercise deleted: id={}", id);
    }

    private WorkoutSheetExercise findEntityById(UUID id) {
        return workoutSheetExerciseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item da ficha não encontrado: " + id));
    }

    private WorkoutSheet findActiveWorkoutSheet(UUID workoutSheetId) {
        WorkoutSheet workoutSheet = findWorkoutSheet(workoutSheetId);

        ensureWorkoutSheetIsActive(workoutSheet);
        return workoutSheet;
    }

    private WorkoutSheet findWorkoutSheet(UUID workoutSheetId) {
        return workoutSheetRepository.findById(workoutSheetId)
                .orElseThrow(() -> new ResourceNotFoundException("Ficha de treino não encontrada: " + workoutSheetId));
    }

    private Exercise findExercise(Integer exerciseId) {
        return exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ResourceNotFoundException("Exercício não encontrado: " + exerciseId));
    }

    private void ensureWorkoutSheetIsActive(WorkoutSheet workoutSheet) {
        if (Boolean.FALSE.equals(workoutSheet.getActive())) {
            throw new BusinessException("Ficha de treino inativa não pode ser alterada");
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

            throw new AccessDeniedException("Usuário não tem permissão para acessar esta ficha");
    }

    private void ensureInstructorCanManage(
            WorkoutSheet workoutSheet,
            String authenticatedEmail,
            boolean admin) {
        if (admin) {
            return;
        }

        if (authenticatedEmail == null
                || !workoutSheet.getInstructor().getUser().getEmail().equalsIgnoreCase(authenticatedEmail)) {
            throw new AccessDeniedException("Instrutor não tem permissão para alterar esta ficha");
        }
    }

    private void ensureOrderIsAvailable(UUID workoutSheetId, String trainingSection, Integer order, UUID currentItemId) {
        workoutSheetExerciseRepository.findByWorkoutSheetWorkoutSheetIdOrderByTrainingSectionAscExecutionOrderAsc(workoutSheetId)
                .stream()
                .filter(item -> item.getTrainingSection().equalsIgnoreCase(trainingSection))
                .filter(item -> item.getExecutionOrder().equals(order))
                .filter(item -> currentItemId == null || !item.getWorkoutSheetExerciseId().equals(currentItemId))
                .findFirst()
                .ifPresent(item -> {
            throw new BusinessException("Ordem de execução já usada no treino " + trainingSection + ": " + order);
                });
    }

    private String resolveTrainingSection(String trainingSection) {
        return trainingSection == null || trainingSection.isBlank()
                ? DEFAULT_TRAINING_SECTION
                : trainingSection.trim();
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
                .trainingSection(resolveTrainingSection(request.getTrainingSection()))
                .executionOrder(request.getExecutionOrder())
                .notes(request.getNotes())
                .build();
    }
}
