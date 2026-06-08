package com.gymiq.service;

import com.gymiq.aop.Auditable;
import com.gymiq.dto.request.CreateWorkoutBlockRequest;
import com.gymiq.dto.request.CreateWorkoutSheetExerciseRequest;
import com.gymiq.dto.request.CreateWorkoutSheetRequest;
import com.gymiq.dto.response.WorkoutSheetResponse;
import com.gymiq.dto.response.WorkoutSheetSummaryResponse;
import com.gymiq.entity.Exercise;
import com.gymiq.entity.Instructor;
import com.gymiq.entity.Student;
import com.gymiq.entity.WorkoutBlock;
import com.gymiq.entity.WorkoutSheet;
import com.gymiq.entity.WorkoutSheetExercise;
import com.gymiq.enums.AuditAction;
import com.gymiq.enums.ResourceType;
import com.gymiq.exception.BusinessException;
import com.gymiq.exception.ResourceNotFoundException;
import com.gymiq.repository.ExerciseRepository;
import com.gymiq.repository.InstructorRepository;
import com.gymiq.repository.StudentRepository;
import com.gymiq.repository.WorkoutSheetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkoutSheetService {

    private static final String DEFAULT_TRAINING_SECTION = "Treino A";

    private final WorkoutSheetRepository workoutSheetRepository;
    private final StudentRepository studentRepository;
    private final InstructorRepository instructorRepository;
    private final ExerciseRepository exerciseRepository;
    private final PersonalDataProtectionService personalDataProtectionService;

    @Transactional
    public WorkoutSheetResponse create(CreateWorkoutSheetRequest request) {
        return create(request, null, true);
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE_WORKOUT_SHEET, resourceType = ResourceType.WORKOUT_SHEET, description = "Criou ficha de treino")
    public WorkoutSheetResponse create(CreateWorkoutSheetRequest request, String authenticatedEmail, boolean admin) {
        validateDates(request);

        Student student = findActiveStudent(request.getStudentId());
        Instructor instructor = findActiveInstructor(request.getInstructorId());
        ensureInstructorCanManage(instructor, authenticatedEmail, admin);

        WorkoutSheet workoutSheet = WorkoutSheet.builder()
                .student(student)
                .instructor(instructor)
                .name(request.getName())
                .goal(request.getGoal())
                .startDate(resolveStartDate(request.getStartDate()))
                .endDate(request.getEndDate())
                .active(true)
                .notes(request.getNotes())
                .build();

        workoutSheet.setBlocks(new ArrayList<>(buildBlocks(workoutSheet, request)));
        workoutSheetRepository.save(workoutSheet);

        log.info("Workout sheet created: id={}, student={}, instructor={}",
                workoutSheet.getWorkoutSheetId(), student.getStudentId(), instructor.getInstructorId());
        return WorkoutSheetResponse.fromEntity(workoutSheet);
    }

    @Transactional(readOnly = true)
    public Page<WorkoutSheetSummaryResponse> findAll(Pageable pageable) {
        return workoutSheetRepository.findAll(pageable)
                .map(WorkoutSheetSummaryResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public WorkoutSheetResponse findById(UUID id) {
        return WorkoutSheetResponse.fromEntity(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public WorkoutSheetResponse findById(UUID id, String authenticatedEmail, boolean admin) {
        WorkoutSheet workoutSheet = findEntityById(id);
        ensureInstructorCanManage(workoutSheet.getInstructor(), authenticatedEmail, admin);
        return WorkoutSheetResponse.fromEntity(workoutSheet);
    }

    @Transactional(readOnly = true)
    public Page<WorkoutSheetSummaryResponse> findByStudent(UUID studentId, boolean onlyActive, Pageable pageable) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Aluno não encontrado: " + studentId);
        }

        Page<WorkoutSheet> workoutSheets = onlyActive
                ? workoutSheetRepository.findByStudentStudentIdAndActiveTrue(studentId, pageable)
                : workoutSheetRepository.findByStudentStudentId(studentId, pageable);

        return workoutSheets.map(WorkoutSheetSummaryResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<WorkoutSheetSummaryResponse> findByStudent(
            UUID studentId,
            boolean onlyActive,
            Pageable pageable,
            String authenticatedEmail,
            boolean admin) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Aluno não encontrado: " + studentId);
        }

        if (admin) {
            return findByStudent(studentId, onlyActive, pageable);
        }

        Page<WorkoutSheet> workoutSheets = onlyActive
                ? workoutSheetRepository.findByStudentStudentIdAndInstructorUserEmailHashAndActiveTrue(
                        studentId, personalDataProtectionService.emailHash(authenticatedEmail), pageable)
                : workoutSheetRepository.findByStudentStudentIdAndInstructorUserEmailHash(
                        studentId, personalDataProtectionService.emailHash(authenticatedEmail), pageable);

        return workoutSheets.map(WorkoutSheetSummaryResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<WorkoutSheetSummaryResponse> findByAuthenticatedStudent(String email, boolean onlyActive, Pageable pageable) {
        UUID studentId = studentRepository.findByUserEmailHash(personalDataProtectionService.emailHash(email))
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado para o usuário autenticado"))
                .getStudentId();

        return findByStudent(studentId, onlyActive, pageable);
    }

    @Transactional(readOnly = true)
    public Page<WorkoutSheetSummaryResponse> findByInstructor(UUID instructorId, Pageable pageable) {
        if (!instructorRepository.existsById(instructorId)) {
            throw new ResourceNotFoundException("Instrutor não encontrado: " + instructorId);
        }

        return workoutSheetRepository.findByInstructorInstructorId(instructorId, pageable)
                .map(WorkoutSheetSummaryResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<WorkoutSheetSummaryResponse> findByInstructor(
            UUID instructorId,
            Pageable pageable,
            String authenticatedEmail,
            boolean admin) {
        Instructor instructor = instructorRepository.findById(instructorId)
                .orElseThrow(() -> new ResourceNotFoundException("Instrutor não encontrado: " + instructorId));

        ensureInstructorCanManage(instructor, authenticatedEmail, admin);

        return workoutSheetRepository.findByInstructorInstructorId(instructorId, pageable)
                .map(WorkoutSheetSummaryResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<WorkoutSheetSummaryResponse> findByAuthenticatedInstructor(String email, Pageable pageable) {
        UUID instructorId = instructorRepository.findByUserEmailHash(personalDataProtectionService.emailHash(email))
                .orElseThrow(() -> new ResourceNotFoundException("Instrutor não encontrado para o usuário autenticado"))
                .getInstructorId();

        return workoutSheetRepository.findByInstructorInstructorId(instructorId, pageable)
                .map(WorkoutSheetSummaryResponse::fromEntity);
    }

    @Transactional
    public WorkoutSheetResponse update(UUID id, CreateWorkoutSheetRequest request) {
        return update(id, request, null, true);
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE_WORKOUT_SHEET, resourceType = ResourceType.WORKOUT_SHEET, description = "Atualizou ficha de treino")
    public WorkoutSheetResponse update(
            UUID id,
            CreateWorkoutSheetRequest request,
            String authenticatedEmail,
            boolean admin) {
        validateDates(request);

        WorkoutSheet workoutSheet = findEntityById(id);
        ensureInstructorCanManage(workoutSheet.getInstructor(), authenticatedEmail, admin);

        Student student = findActiveStudent(request.getStudentId());
        Instructor instructor = findActiveInstructor(request.getInstructorId());
        ensureInstructorCanManage(instructor, authenticatedEmail, admin);

        workoutSheet.setStudent(student);
        workoutSheet.setInstructor(instructor);
        workoutSheet.setName(request.getName());
        workoutSheet.setGoal(request.getGoal());
        workoutSheet.setStartDate(resolveStartDate(request.getStartDate()));
        workoutSheet.setEndDate(request.getEndDate());
        workoutSheet.setNotes(request.getNotes());
        workoutSheet.getBlocks().clear();
        workoutSheet.getBlocks().addAll(buildBlocks(workoutSheet, request));

        workoutSheetRepository.save(workoutSheet);
        log.info("Workout sheet updated: id={}", id);
        return WorkoutSheetResponse.fromEntity(workoutSheet);
    }

    @Transactional
    public void deactivate(UUID id) {
        deactivate(id, null, true);
    }

    @Transactional
    @Auditable(action = AuditAction.DEACTIVATE_WORKOUT_SHEET, resourceType = ResourceType.WORKOUT_SHEET, description = "Inativou ficha de treino")
    public void deactivate(UUID id, String authenticatedEmail, boolean admin) {
        WorkoutSheet workoutSheet = findEntityById(id);
        ensureInstructorCanManage(workoutSheet.getInstructor(), authenticatedEmail, admin);
        workoutSheet.setActive(false);
        workoutSheetRepository.save(workoutSheet);
        log.info("Workout sheet deactivated: id={}", id);
    }

    WorkoutSheet findEntityById(UUID id) {
        return workoutSheetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ficha de treino não encontrada: " + id));
    }

    void ensureWorkoutSheetIsActive(WorkoutSheet workoutSheet) {
        if (Boolean.FALSE.equals(workoutSheet.getActive())) {
            throw new BusinessException("Ficha de treino inativa não pode ser alterada");
        }
    }

    void ensureCanViewWorkoutSheet(
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

    void ensureInstructorCanManage(WorkoutSheet workoutSheet, String authenticatedEmail, boolean admin) {
        ensureInstructorCanManage(workoutSheet.getInstructor(), authenticatedEmail, admin);
    }

    private Student findActiveStudent(UUID studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado: " + studentId));

        if (Boolean.FALSE.equals(student.getUser().getActive())) {
            throw new BusinessException("Aluno inativo não pode receber ficha de treino");
        }
        return student;
    }

    private Instructor findActiveInstructor(UUID instructorId) {
        Instructor instructor = instructorRepository.findById(instructorId)
                .orElseThrow(() -> new ResourceNotFoundException("Instrutor não encontrado: " + instructorId));

        if (Boolean.FALSE.equals(instructor.getUser().getActive())) {
            throw new BusinessException("Instrutor inativo não pode criar ficha de treino");
        }
        return instructor;
    }

    private void ensureInstructorCanManage(Instructor instructor, String authenticatedEmail, boolean admin) {
        if (admin) {
            return;
        }

        if (authenticatedEmail == null
                || !instructor.getUser().getEmail().equalsIgnoreCase(authenticatedEmail)) {
            throw new AccessDeniedException("Instrutor não tem permissão para acessar esta ficha");
        }
    }

    private List<WorkoutBlock> buildBlocks(WorkoutSheet workoutSheet, CreateWorkoutSheetRequest request) {
        List<BlockDraft> blockDrafts = resolveBlockDrafts(request);
        validateBlockDrafts(blockDrafts);

        return blockDrafts.stream()
                .map(blockDraft -> buildBlock(workoutSheet, blockDraft))
                .toList();
    }

    private WorkoutBlock buildBlock(WorkoutSheet workoutSheet, BlockDraft blockDraft) {
        WorkoutBlock block = WorkoutBlock.builder()
                .workoutSheet(workoutSheet)
                .name(blockDraft.name())
                .description(blockDraft.description())
                .executionOrder(blockDraft.executionOrder())
                .active(true)
                .build();

        block.setExercises(new ArrayList<>(blockDraft.exercises()
                .stream()
                .map(itemRequest -> buildExerciseItem(block, itemRequest))
                .toList()));

        return block;
    }

    private WorkoutSheetExercise buildExerciseItem(
            WorkoutBlock workoutBlock,
            CreateWorkoutSheetExerciseRequest request) {

        Exercise exercise = exerciseRepository.findById(request.getExerciseId())
                .orElseThrow(() -> new ResourceNotFoundException("Exercício não encontrado: " + request.getExerciseId()));

        return WorkoutSheetExercise.builder()
                .workoutBlock(workoutBlock)
                .exercise(exercise)
                .sets(request.getSets())
                .repetitions(request.getRepetitions())
                .restSeconds(request.getRestSeconds())
                .executionOrder(request.getExecutionOrder())
                .notes(request.getNotes())
                .build();
    }

    private List<BlockDraft> resolveBlockDrafts(CreateWorkoutSheetRequest request) {
        if (request.getBlocks() != null && !request.getBlocks().isEmpty()) {
            return request.getBlocks()
                    .stream()
                    .map(block -> new BlockDraft(
                            normalizeName(block.getName()),
                            block.getDescription(),
                            block.getExecutionOrder(),
                            block.getExercises() == null ? List.of() : block.getExercises()))
                    .toList();
        }

        if (request.getExercises() == null || request.getExercises().isEmpty()) {
            throw new BusinessException("A ficha deve possuir pelo menos um treino com exercícios");
        }

        Map<String, List<CreateWorkoutSheetExerciseRequest>> groupedExercises = new LinkedHashMap<>();
        request.getExercises().forEach(exercise -> groupedExercises
                .computeIfAbsent(resolveTrainingSection(exercise.getTrainingSection()), key -> new ArrayList<>())
                .add(exercise));

        List<BlockDraft> drafts = new ArrayList<>();
        int order = 1;
        for (Map.Entry<String, List<CreateWorkoutSheetExerciseRequest>> entry : groupedExercises.entrySet()) {
            drafts.add(new BlockDraft(entry.getKey(), null, order, entry.getValue()));
            order++;
        }
        return drafts;
    }

    private void validateBlockDrafts(List<BlockDraft> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            throw new BusinessException("A ficha deve possuir pelo menos um treino");
        }

        Set<String> blockNames = new HashSet<>();
        Set<Integer> blockOrders = new HashSet<>();
        for (BlockDraft block : blocks) {
            if (block.name() == null || block.name().isBlank()) {
                throw new BusinessException("Nome do treino é obrigatório");
            }
            if (block.executionOrder() == null) {
                throw new BusinessException("Ordem do treino é obrigatória");
            }
            if (!blockNames.add(block.name().toLowerCase())) {
                throw new BusinessException("Nome de treino duplicado: " + block.name());
            }
            if (!blockOrders.add(block.executionOrder())) {
                throw new BusinessException("Ordem de treino duplicada: " + block.executionOrder());
            }
            validateExerciseOrders(block);
        }
    }

    private void validateExerciseOrders(BlockDraft block) {
        if (block.exercises() == null || block.exercises().isEmpty()) {
            return;
        }

        Set<Integer> orders = new HashSet<>();
        for (CreateWorkoutSheetExerciseRequest exercise : block.exercises()) {
            if (!orders.add(exercise.getExecutionOrder())) {
                throw new BusinessException("Ordem de execução duplicada no treino "
                        + block.name() + ": " + exercise.getExecutionOrder());
            }
        }
    }

    private void validateDates(CreateWorkoutSheetRequest request) {
        if (request.getEndDate() == null) {
            return;
        }

        LocalDate startDate = resolveStartDate(request.getStartDate());
        if (request.getEndDate().isBefore(startDate)) {
            throw new BusinessException("Data final não pode ser anterior à data inicial");
        }
    }

    private String resolveTrainingSection(String trainingSection) {
        return trainingSection == null || trainingSection.isBlank()
                ? DEFAULT_TRAINING_SECTION
                : normalizeName(trainingSection);
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }

    private LocalDate resolveStartDate(LocalDate startDate) {
        return startDate != null ? startDate : LocalDate.now();
    }

    private record BlockDraft(
            String name,
            String description,
            Integer executionOrder,
            List<CreateWorkoutSheetExerciseRequest> exercises) {
    }
}
