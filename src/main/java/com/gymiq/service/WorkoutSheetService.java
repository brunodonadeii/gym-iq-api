package com.gymiq.service;

import com.gymiq.dto.request.CreateWorkoutSheetExerciseRequest;
import com.gymiq.dto.request.CreateWorkoutSheetRequest;
import com.gymiq.dto.response.WorkoutSheetResponse;
import com.gymiq.entity.Exercise;
import com.gymiq.entity.Instructor;
import com.gymiq.entity.Student;
import com.gymiq.entity.WorkoutSheet;
import com.gymiq.entity.WorkoutSheetExercise;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkoutSheetService {

    private final WorkoutSheetRepository workoutSheetRepository;
    private final StudentRepository studentRepository;
    private final InstructorRepository instructorRepository;
    private final ExerciseRepository exerciseRepository;

    @Transactional
    public WorkoutSheetResponse create(CreateWorkoutSheetRequest request) {
        validateDates(request);
        validateExerciseOrders(request.getExercises());

        Student student = findActiveStudent(request.getStudentId());
        Instructor instructor = findActiveInstructor(request.getInstructorId());

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

        workoutSheet.setExercises(new ArrayList<>(buildExerciseItems(workoutSheet, request.getExercises())));
        workoutSheetRepository.save(workoutSheet);

        log.info("Workout sheet created: id={}, student={}, instructor={}",
                workoutSheet.getWorkoutSheetId(), student.getStudentId(), instructor.getInstructorId());
        return WorkoutSheetResponse.fromEntity(workoutSheet);
    }

    @Transactional(readOnly = true)
    public Page<WorkoutSheetResponse> findAll(Pageable pageable) {
        return workoutSheetRepository.findAll(pageable)
                .map(WorkoutSheetResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public WorkoutSheetResponse findById(Integer id) {
        return WorkoutSheetResponse.fromEntity(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public Page<WorkoutSheetResponse> findByStudent(Integer studentId, boolean onlyActive, Pageable pageable) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Aluno nao encontrado: " + studentId);
        }

        Page<WorkoutSheet> workoutSheets = onlyActive
                ? workoutSheetRepository.findByStudentStudentIdAndActiveTrue(studentId, pageable)
                : workoutSheetRepository.findByStudentStudentId(studentId, pageable);

        return workoutSheets.map(WorkoutSheetResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<WorkoutSheetResponse> findByAuthenticatedStudent(String email, boolean onlyActive, Pageable pageable) {
        Integer studentId = studentRepository.findByUserEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno nao encontrado para o usuario autenticado"))
                .getStudentId();

        return findByStudent(studentId, onlyActive, pageable);
    }

    @Transactional(readOnly = true)
    public Page<WorkoutSheetResponse> findByInstructor(Integer instructorId, Pageable pageable) {
        if (!instructorRepository.existsById(instructorId)) {
            throw new ResourceNotFoundException("Instrutor nao encontrado: " + instructorId);
        }

        return workoutSheetRepository.findByInstructorInstructorId(instructorId, pageable)
                .map(WorkoutSheetResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<WorkoutSheetResponse> findByAuthenticatedInstructor(String email, Pageable pageable) {
        Integer instructorId = instructorRepository.findByUserEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Instrutor nao encontrado para o usuario autenticado"))
                .getInstructorId();

        return workoutSheetRepository.findByInstructorInstructorId(instructorId, pageable)
                .map(WorkoutSheetResponse::fromEntity);
    }

    @Transactional
    public WorkoutSheetResponse update(Integer id, CreateWorkoutSheetRequest request) {
        validateDates(request);
        validateExerciseOrders(request.getExercises());

        WorkoutSheet workoutSheet = findEntityById(id);
        Student student = findActiveStudent(request.getStudentId());
        Instructor instructor = findActiveInstructor(request.getInstructorId());

        workoutSheet.setStudent(student);
        workoutSheet.setInstructor(instructor);
        workoutSheet.setName(request.getName());
        workoutSheet.setGoal(request.getGoal());
        workoutSheet.setStartDate(resolveStartDate(request.getStartDate()));
        workoutSheet.setEndDate(request.getEndDate());
        workoutSheet.setNotes(request.getNotes());
        workoutSheet.getExercises().clear();
        workoutSheet.getExercises().addAll(buildExerciseItems(workoutSheet, request.getExercises()));

        workoutSheetRepository.save(workoutSheet);
        log.info("Workout sheet updated: id={}", id);
        return WorkoutSheetResponse.fromEntity(workoutSheet);
    }

    @Transactional
    public void deactivate(Integer id) {
        WorkoutSheet workoutSheet = findEntityById(id);
        workoutSheet.setActive(false);
        workoutSheetRepository.save(workoutSheet);
        log.info("Workout sheet deactivated: id={}", id);
    }

    private WorkoutSheet findEntityById(Integer id) {
        return workoutSheetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ficha de treino nao encontrada: " + id));
    }

    private Student findActiveStudent(Integer studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno nao encontrado: " + studentId));

        if (Boolean.FALSE.equals(student.getUser().getActive())) {
            throw new BusinessException("Aluno inativo nao pode receber ficha de treino");
        }
        return student;
    }

    private Instructor findActiveInstructor(Integer instructorId) {
        Instructor instructor = instructorRepository.findById(instructorId)
                .orElseThrow(() -> new ResourceNotFoundException("Instrutor nao encontrado: " + instructorId));

        if (Boolean.FALSE.equals(instructor.getUser().getActive())) {
            throw new BusinessException("Instrutor inativo nao pode criar ficha de treino");
        }
        return instructor;
    }

    private List<WorkoutSheetExercise> buildExerciseItems(
            WorkoutSheet workoutSheet,
            List<CreateWorkoutSheetExerciseRequest> itemRequests) {

        return itemRequests.stream()
                .map(itemRequest -> buildExerciseItem(workoutSheet, itemRequest))
                .toList();
    }

    private WorkoutSheetExercise buildExerciseItem(
            WorkoutSheet workoutSheet,
            CreateWorkoutSheetExerciseRequest request) {

        Exercise exercise = exerciseRepository.findById(request.getExerciseId())
                .orElseThrow(() -> new ResourceNotFoundException("Exercicio nao encontrado: " + request.getExerciseId()));

        if (Boolean.FALSE.equals(exercise.getActive())) {
            throw new BusinessException("Exercicio inativo nao pode ser usado em ficha de treino: " + exercise.getName());
        }

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

    private void validateDates(CreateWorkoutSheetRequest request) {
        if (request.getEndDate() == null) {
            return;
        }

        LocalDate startDate = resolveStartDate(request.getStartDate());
        if (request.getEndDate().isBefore(startDate)) {
            throw new BusinessException("Data final nao pode ser anterior a data inicial");
        }
    }

    private void validateExerciseOrders(List<CreateWorkoutSheetExerciseRequest> exercises) {
        if (exercises == null) {
            throw new BusinessException("A ficha deve possuir pelo menos um exercicio");
        }

        Set<Integer> orders = new HashSet<>();
        for (CreateWorkoutSheetExerciseRequest exercise : exercises) {
            if (!orders.add(exercise.getExecutionOrder())) {
                throw new BusinessException("Ordem de execucao duplicada: " + exercise.getExecutionOrder());
            }
        }
    }

    private LocalDate resolveStartDate(LocalDate startDate) {
        return startDate != null ? startDate : LocalDate.now();
    }
}
