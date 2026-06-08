package com.gymiq.service;

import com.gymiq.aop.Auditable;
import com.gymiq.dto.request.CreateWorkoutSheetExerciseRequest;
import com.gymiq.dto.response.WorkoutSheetExerciseResponse;
import com.gymiq.entity.Exercise;
import com.gymiq.entity.WorkoutBlock;
import com.gymiq.entity.WorkoutSheet;
import com.gymiq.entity.WorkoutSheetExercise;
import com.gymiq.enums.AuditAction;
import com.gymiq.enums.ResourceType;
import com.gymiq.exception.BusinessException;
import com.gymiq.exception.ResourceNotFoundException;
import com.gymiq.repository.ExerciseRepository;
import com.gymiq.repository.WorkoutBlockRepository;
import com.gymiq.repository.WorkoutSheetExerciseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkoutSheetExerciseService {

    private static final String DEFAULT_TRAINING_SECTION = "Treino A";

    private final WorkoutSheetExerciseRepository workoutSheetExerciseRepository;
    private final WorkoutBlockRepository workoutBlockRepository;
    private final WorkoutSheetService workoutSheetService;
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
        WorkoutSheet workoutSheet = workoutSheetService.findEntityById(workoutSheetId);
        workoutSheetService.ensureWorkoutSheetIsActive(workoutSheet);
        workoutSheetService.ensureInstructorCanManage(workoutSheet, authenticatedEmail, admin);

        WorkoutBlock block = findOrCreateBlock(workoutSheet, resolveTrainingSection(request.getTrainingSection()));
        return addExerciseToBlock(block, request);
    }

    @Transactional
    public WorkoutSheetExerciseResponse addExerciseToBlock(UUID workoutBlockId, CreateWorkoutSheetExerciseRequest request) {
        return addExerciseToBlock(workoutBlockId, request, null, true);
    }

    @Transactional
    @Auditable(action = AuditAction.ADD_WORKOUT_SHEET_EXERCISE, resourceType = ResourceType.WORKOUT_SHEET_EXERCISE, description = "Adicionou exercicio no treino")
    public WorkoutSheetExerciseResponse addExerciseToBlock(
            UUID workoutBlockId,
            CreateWorkoutSheetExerciseRequest request,
            String authenticatedEmail,
            boolean admin) {
        WorkoutBlock block = findActiveBlock(workoutBlockId);
        workoutSheetService.ensureInstructorCanManage(block.getWorkoutSheet(), authenticatedEmail, admin);
        return addExerciseToBlock(block, request);
    }

    @Transactional(readOnly = true)
    public Page<WorkoutSheetExerciseResponse> findByWorkoutSheet(UUID workoutSheetId, Pageable pageable) {
        workoutSheetService.findEntityById(workoutSheetId);

        return workoutSheetExerciseRepository.findByWorkoutBlockWorkoutSheetWorkoutSheetId(workoutSheetId, pageable)
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
        WorkoutSheet workoutSheet = workoutSheetService.findEntityById(workoutSheetId);
        workoutSheetService.ensureCanViewWorkoutSheet(workoutSheet, authenticatedEmail, admin, instructor, student);

        return workoutSheetExerciseRepository.findByWorkoutBlockWorkoutSheetWorkoutSheetId(workoutSheetId, pageable)
                .map(WorkoutSheetExerciseResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<WorkoutSheetExerciseResponse> findByWorkoutBlock(
            UUID workoutBlockId,
            Pageable pageable,
            String authenticatedEmail,
            boolean admin,
            boolean instructor,
            boolean student) {
        WorkoutBlock block = findBlock(workoutBlockId);
        workoutSheetService.ensureCanViewWorkoutSheet(block.getWorkoutSheet(), authenticatedEmail, admin, instructor, student);

        return workoutSheetExerciseRepository.findByWorkoutBlockWorkoutBlockId(workoutBlockId, pageable)
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
        WorkoutSheet workoutSheet = item.getWorkoutBlock().getWorkoutSheet();
        Exercise exercise = findExercise(request.getExerciseId());

        workoutSheetService.ensureWorkoutSheetIsActive(workoutSheet);
        workoutSheetService.ensureInstructorCanManage(workoutSheet, authenticatedEmail, admin);

        WorkoutBlock block = item.getWorkoutBlock();
        if (request.getTrainingSection() != null && !request.getTrainingSection().isBlank()
                && !block.getName().equalsIgnoreCase(request.getTrainingSection())) {
            block = findOrCreateBlock(workoutSheet, resolveTrainingSection(request.getTrainingSection()));
        }

        ensureBlockIsActive(block);
        ensureOrderIsAvailable(block.getWorkoutBlockId(), request.getExecutionOrder(), id);

        item.setWorkoutBlock(block);
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
    public void delete(UUID id) {
        delete(id, null, true);
    }

    @Transactional
    @Auditable(action = AuditAction.DELETE_WORKOUT_SHEET_EXERCISE, resourceType = ResourceType.WORKOUT_SHEET_EXERCISE, description = "Removeu exercicio da ficha")
    public void delete(UUID id, String authenticatedEmail, boolean admin) {
        WorkoutSheetExercise item = findEntityById(id);
        workoutSheetService.ensureWorkoutSheetIsActive(item.getWorkoutBlock().getWorkoutSheet());
        workoutSheetService.ensureInstructorCanManage(item.getWorkoutBlock().getWorkoutSheet(), authenticatedEmail, admin);
        workoutSheetExerciseRepository.delete(item);
        log.info("Workout sheet exercise deleted: id={}", id);
    }

    private WorkoutSheetExerciseResponse addExerciseToBlock(WorkoutBlock block, CreateWorkoutSheetExerciseRequest request) {
        ensureBlockIsActive(block);
        Exercise exercise = findExercise(request.getExerciseId());
        ensureOrderIsAvailable(block.getWorkoutBlockId(), request.getExecutionOrder(), null);

        WorkoutSheetExercise item = WorkoutSheetExercise.builder()
                .workoutBlock(block)
                .exercise(exercise)
                .sets(request.getSets())
                .repetitions(request.getRepetitions())
                .restSeconds(request.getRestSeconds())
                .executionOrder(request.getExecutionOrder())
                .notes(request.getNotes())
                .build();

        workoutSheetExerciseRepository.save(item);

        log.info("Workout sheet exercise added: id={}, block={}",
                item.getWorkoutSheetExerciseId(), block.getWorkoutBlockId());
        return WorkoutSheetExerciseResponse.fromEntity(item);
    }

    private WorkoutSheetExercise findEntityById(UUID id) {
        return workoutSheetExerciseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item da ficha não encontrado: " + id));
    }

    private WorkoutBlock findActiveBlock(UUID workoutBlockId) {
        WorkoutBlock block = findBlock(workoutBlockId);
        workoutSheetService.ensureWorkoutSheetIsActive(block.getWorkoutSheet());
        ensureBlockIsActive(block);
        return block;
    }

    private WorkoutBlock findBlock(UUID workoutBlockId) {
        return workoutBlockRepository.findById(workoutBlockId)
                .orElseThrow(() -> new ResourceNotFoundException("Treino não encontrado: " + workoutBlockId));
    }

    private Exercise findExercise(Integer exerciseId) {
        return exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ResourceNotFoundException("Exercício não encontrado: " + exerciseId));
    }

    private WorkoutBlock findOrCreateBlock(WorkoutSheet workoutSheet, String blockName) {
        return workoutBlockRepository
                .findByWorkoutSheetWorkoutSheetIdAndNameIgnoreCase(workoutSheet.getWorkoutSheetId(), blockName)
                .orElseGet(() -> createBlock(workoutSheet, blockName));
    }

    private WorkoutBlock createBlock(WorkoutSheet workoutSheet, String blockName) {
        int nextOrder = workoutBlockRepository
                .findByWorkoutSheetWorkoutSheetIdOrderByExecutionOrderAsc(workoutSheet.getWorkoutSheetId())
                .stream()
                .mapToInt(WorkoutBlock::getExecutionOrder)
                .max()
                .orElse(0) + 1;

        WorkoutBlock block = WorkoutBlock.builder()
                .workoutSheet(workoutSheet)
                .name(blockName)
                .executionOrder(nextOrder)
                .active(true)
                .build();
        return workoutBlockRepository.save(block);
    }

    private void ensureBlockIsActive(WorkoutBlock block) {
        if (Boolean.FALSE.equals(block.getActive())) {
            throw new BusinessException("Treino inativo não pode ser alterado");
        }
    }

    private void ensureOrderIsAvailable(UUID workoutBlockId, Integer order, UUID currentItemId) {
        workoutSheetExerciseRepository.findByWorkoutBlockWorkoutBlockId(workoutBlockId, Pageable.unpaged())
                .stream()
                .filter(item -> item.getExecutionOrder().equals(order))
                .filter(item -> currentItemId == null || !item.getWorkoutSheetExerciseId().equals(currentItemId))
                .findFirst()
                .ifPresent(item -> {
                    throw new BusinessException("Ordem de execução já usada neste treino: " + order);
                });
    }

    private String resolveTrainingSection(String trainingSection) {
        return trainingSection == null || trainingSection.isBlank()
                ? DEFAULT_TRAINING_SECTION
                : trainingSection.trim();
    }
}
