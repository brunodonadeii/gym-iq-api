package com.gymiq.service;

import com.gymiq.dto.request.CreateWorkoutSheetExerciseRequest;
import com.gymiq.dto.request.CreateWorkoutSheetRequest;
import com.gymiq.dto.response.WorkoutSheetResponse;
import com.gymiq.entity.Instructor;
import com.gymiq.entity.Student;
import com.gymiq.entity.WorkoutSheet;
import com.gymiq.exception.BusinessException;
import com.gymiq.repository.ExerciseRepository;
import com.gymiq.repository.InstructorRepository;
import com.gymiq.repository.StudentRepository;
import com.gymiq.repository.WorkoutSheetRepository;
import com.gymiq.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutSheetServiceTest {

    @Mock
    private WorkoutSheetRepository workoutSheetRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private InstructorRepository instructorRepository;

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private PersonalDataProtectionService personalDataProtectionService;

    @InjectMocks
    private WorkoutSheetService workoutSheetService;

    @Test
    void createShouldPersistWorkoutSheetWithExercises() {
        Student student = TestDataFactory.activeStudent();
        Instructor instructor = TestDataFactory.activeInstructor();
        CreateWorkoutSheetRequest request = validRequest(student, instructor);

        when(studentRepository.findById(student.getStudentId())).thenReturn(Optional.of(student));
        when(instructorRepository.findById(instructor.getInstructorId())).thenReturn(Optional.of(instructor));
        when(exerciseRepository.findById(7)).thenReturn(Optional.of(TestDataFactory.exercise()));
        when(workoutSheetRepository.save(any(WorkoutSheet.class))).thenAnswer(invocation -> {
            WorkoutSheet workoutSheet = invocation.getArgument(0);
            workoutSheet.setWorkoutSheetId(TestDataFactory.workoutSheet().getWorkoutSheetId());
            return workoutSheet;
        });

        WorkoutSheetResponse response = workoutSheetService.create(request, instructor.getUser().getEmail(), false);

        assertThat(response.getName()).isEqualTo("Ficha Hipertrofia");
        assertThat(response.getExercises()).hasSize(1);
        assertThat(response.getInstructorId()).isEqualTo(instructor.getInstructorId());
        verify(workoutSheetRepository).save(any(WorkoutSheet.class));
    }

    @Test
    void createShouldRejectDuplicatedExecutionOrderInSameTrainingSection() {
        Student student = TestDataFactory.activeStudent();
        Instructor instructor = TestDataFactory.activeInstructor();
        CreateWorkoutSheetRequest request = validRequest(student, instructor);
        request.setExercises(List.of(exerciseItem("A", 1), exerciseItem("A", 1)));

        when(studentRepository.findById(student.getStudentId())).thenReturn(Optional.of(student));
        when(instructorRepository.findById(instructor.getInstructorId())).thenReturn(Optional.of(instructor));

        assertThatThrownBy(() -> workoutSheetService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("duplicada");
    }

    @Test
    void findByIdShouldRejectInstructorWithoutOwnership() {
        WorkoutSheet workoutSheet = TestDataFactory.workoutSheet();

        when(workoutSheetRepository.findById(workoutSheet.getWorkoutSheetId())).thenReturn(Optional.of(workoutSheet));

        assertThatThrownBy(() -> workoutSheetService.findById(workoutSheet.getWorkoutSheetId(), "outro@gymiq.com", false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deactivateShouldMarkWorkoutSheetAsInactive() {
        WorkoutSheet workoutSheet = TestDataFactory.workoutSheet();

        when(workoutSheetRepository.findById(workoutSheet.getWorkoutSheetId())).thenReturn(Optional.of(workoutSheet));

        workoutSheetService.deactivate(workoutSheet.getWorkoutSheetId());

        assertThat(workoutSheet.getActive()).isFalse();
        verify(workoutSheetRepository).save(workoutSheet);
    }

    private CreateWorkoutSheetRequest validRequest(Student student, Instructor instructor) {
        CreateWorkoutSheetRequest request = new CreateWorkoutSheetRequest();
        request.setStudentId(student.getStudentId());
        request.setInstructorId(instructor.getInstructorId());
        request.setName("Ficha Hipertrofia");
        request.setGoal("Ganho de massa");
        request.setStartDate(LocalDate.of(2026, 5, 1));
        request.setEndDate(LocalDate.of(2026, 8, 1));
        request.setNotes("Ajustar cargas semanalmente");
        request.setExercises(List.of(exerciseItem("A", 1)));
        return request;
    }

    private CreateWorkoutSheetExerciseRequest exerciseItem(String section, int order) {
        CreateWorkoutSheetExerciseRequest request = new CreateWorkoutSheetExerciseRequest();
        request.setExerciseId(7);
        request.setSets(4);
        request.setRepetitions("10");
        request.setRestSeconds(60);
        request.setTrainingSection(section);
        request.setExecutionOrder(order);
        request.setNotes("Controlar movimento");
        return request;
    }
}
