package com.gymiq.service;

import com.gymiq.aop.Auditable;
import com.gymiq.dto.request.CreateWorkoutBlockRequest;
import com.gymiq.dto.response.WorkoutBlockResponse;
import com.gymiq.dto.response.WorkoutBlockSummaryResponse;
import com.gymiq.entity.WorkoutBlock;
import com.gymiq.entity.WorkoutSheet;
import com.gymiq.entity.WorkoutSheetExercise;
import com.gymiq.enums.AuditAction;
import com.gymiq.enums.ResourceType;
import com.gymiq.exception.BusinessException;
import com.gymiq.exception.ResourceNotFoundException;
import com.gymiq.repository.ExerciseRepository;
import com.gymiq.repository.WorkoutBlockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkoutBlockService {

    private final WorkoutBlockRepository workoutBlockRepository;
    private final ExerciseRepository exerciseRepository;
    private final WorkoutSheetService workoutSheetService;

    @Transactional
    @Auditable(action = AuditAction.CREATE_WORKOUT_BLOCK, resourceType = ResourceType.WORKOUT_BLOCK, description = "Criou treino na ficha")
    public WorkoutBlockResponse create(
            UUID workoutSheetId,
            CreateWorkoutBlockRequest request,
            String authenticatedEmail,
            boolean admin) {
        WorkoutSheet workoutSheet = workoutSheetService.findEntityById(workoutSheetId);
        workoutSheetService.ensureWorkoutSheetIsActive(workoutSheet);
        workoutSheetService.ensureInstructorCanManage(workoutSheet, authenticatedEmail, admin);
        ensureNameIsAvailable(workoutSheetId, request.getName(), null);
        ensureOrderIsAvailable(workoutSheetId, request.getExecutionOrder(), null);

        WorkoutBlock block = WorkoutBlock.builder()
                .workoutSheet(workoutSheet)
                .name(request.getName().trim())
                .description(request.getDescription())
                .executionOrder(request.getExecutionOrder())
                .active(true)
                .build();

        if (request.getExercises() != null) {
            block.setExercises(new ArrayList<>(request.getExercises()
                    .stream()
                    .map(itemRequest -> WorkoutSheetExercise.builder()
                            .workoutBlock(block)
                            .exercise(exerciseRepository.findById(itemRequest.getExerciseId())
                                    .orElseThrow(() -> new ResourceNotFoundException("Exercício não encontrado: " + itemRequest.getExerciseId())))
                            .sets(itemRequest.getSets())
                            .repetitions(itemRequest.getRepetitions())
                            .restSeconds(itemRequest.getRestSeconds())
                            .executionOrder(itemRequest.getExecutionOrder())
                            .notes(itemRequest.getNotes())
                            .build())
                    .toList()));
        }

        validateExerciseOrders(block);
        workoutBlockRepository.save(block);
        log.info("Workout block created: id={}, sheet={}", block.getWorkoutBlockId(), workoutSheetId);
        return WorkoutBlockResponse.fromEntity(block);
    }

    @Transactional(readOnly = true)
    public Page<WorkoutBlockSummaryResponse> findByWorkoutSheet(
            UUID workoutSheetId,
            Pageable pageable,
            String authenticatedEmail,
            boolean admin,
            boolean instructor,
            boolean student) {
        WorkoutSheet workoutSheet = workoutSheetService.findEntityById(workoutSheetId);
        workoutSheetService.ensureCanViewWorkoutSheet(workoutSheet, authenticatedEmail, admin, instructor, student);

        return workoutBlockRepository.findByWorkoutSheetWorkoutSheetId(workoutSheetId, pageable)
                .map(WorkoutBlockSummaryResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public WorkoutBlockResponse findById(
            UUID id,
            String authenticatedEmail,
            boolean admin,
            boolean instructor,
            boolean student) {
        WorkoutBlock block = findEntityById(id);
        workoutSheetService.ensureCanViewWorkoutSheet(block.getWorkoutSheet(), authenticatedEmail, admin, instructor, student);
        return WorkoutBlockResponse.fromEntity(block);
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE_WORKOUT_BLOCK, resourceType = ResourceType.WORKOUT_BLOCK, description = "Atualizou treino da ficha")
    public WorkoutBlockResponse update(
            UUID id,
            CreateWorkoutBlockRequest request,
            String authenticatedEmail,
            boolean admin) {
        WorkoutBlock block = findEntityById(id);
        WorkoutSheet workoutSheet = block.getWorkoutSheet();
        workoutSheetService.ensureWorkoutSheetIsActive(workoutSheet);
        workoutSheetService.ensureInstructorCanManage(workoutSheet, authenticatedEmail, admin);
        ensureNameIsAvailable(workoutSheet.getWorkoutSheetId(), request.getName(), id);
        ensureOrderIsAvailable(workoutSheet.getWorkoutSheetId(), request.getExecutionOrder(), id);

        block.setName(request.getName().trim());
        block.setDescription(request.getDescription());
        block.setExecutionOrder(request.getExecutionOrder());
        workoutBlockRepository.save(block);

        log.info("Workout block updated: id={}", id);
        return WorkoutBlockResponse.fromEntity(block);
    }

    @Transactional
    @Auditable(action = AuditAction.DELETE_WORKOUT_BLOCK, resourceType = ResourceType.WORKOUT_BLOCK, description = "Removeu treino da ficha")
    public void delete(UUID id, String authenticatedEmail, boolean admin) {
        WorkoutBlock block = findEntityById(id);
        workoutSheetService.ensureWorkoutSheetIsActive(block.getWorkoutSheet());
        workoutSheetService.ensureInstructorCanManage(block.getWorkoutSheet(), authenticatedEmail, admin);
        workoutBlockRepository.delete(block);
        log.info("Workout block deleted: id={}", id);
    }

    private WorkoutBlock findEntityById(UUID id) {
        return workoutBlockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Treino não encontrado: " + id));
    }

    private void ensureNameIsAvailable(UUID workoutSheetId, String name, UUID currentBlockId) {
        workoutBlockRepository.findByWorkoutSheetWorkoutSheetIdAndNameIgnoreCase(workoutSheetId, name.trim())
                .filter(block -> currentBlockId == null || !block.getWorkoutBlockId().equals(currentBlockId))
                .ifPresent(block -> {
                    throw new BusinessException("Já existe um treino com o nome: " + name);
                });
    }

    private void ensureOrderIsAvailable(UUID workoutSheetId, Integer order, UUID currentBlockId) {
        workoutBlockRepository.findByWorkoutSheetWorkoutSheetIdOrderByExecutionOrderAsc(workoutSheetId)
                .stream()
                .filter(block -> block.getExecutionOrder().equals(order))
                .filter(block -> currentBlockId == null || !block.getWorkoutBlockId().equals(currentBlockId))
                .findFirst()
                .ifPresent(block -> {
                    throw new BusinessException("Ordem de treino já utilizada: " + order);
                });
    }

    private void validateExerciseOrders(WorkoutBlock block) {
        long distinctOrders = block.getExercises()
                .stream()
                .map(WorkoutSheetExercise::getExecutionOrder)
                .distinct()
                .count();

        if (distinctOrders != block.getExercises().size()) {
            throw new BusinessException("Ordem de execução duplicada no treino " + block.getName());
        }
    }
}
