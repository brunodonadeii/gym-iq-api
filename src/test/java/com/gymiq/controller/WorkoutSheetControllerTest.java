package com.gymiq.controller;

import com.gymiq.dto.request.CreateWorkoutSheetRequest;
import com.gymiq.dto.response.WorkoutSheetResponse;
import com.gymiq.dto.response.WorkoutSheetSummaryResponse;
import com.gymiq.entity.WorkoutSheet;
import com.gymiq.service.WorkoutSheetService;
import com.gymiq.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkoutSheetControllerTest {

    @Test
    void createShouldReturnCreatedWorkoutSheet() {
        WorkoutSheetService service = mock(WorkoutSheetService.class);
        WorkoutSheetController controller = new WorkoutSheetController(service);
        WorkoutSheet sheet = TestDataFactory.workoutSheet();
        CreateWorkoutSheetRequest request = request(sheet);
        Authentication authentication = authentication("admin@gymiq.com", "ROLE_ADMIN");

        when(service.create(request, "admin@gymiq.com", true))
                .thenReturn(WorkoutSheetResponse.fromEntity(sheet));

        ResponseEntity<WorkoutSheetResponse> response = controller.create(authentication, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getWorkoutSheetId()).isEqualTo(sheet.getWorkoutSheetId());
        verify(service).create(request, "admin@gymiq.com", true);
    }

    @Test
    void findAllShouldReturnPagedWorkoutSheets() {
        WorkoutSheetService service = mock(WorkoutSheetService.class);
        WorkoutSheetController controller = new WorkoutSheetController(service);
        WorkoutSheet sheet = TestDataFactory.workoutSheet();

        when(service.findAll(Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(WorkoutSheetSummaryResponse.fromEntity(sheet))));

        ResponseEntity<Page<WorkoutSheetSummaryResponse>> response = controller.findAll(Pageable.unpaged());

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        verify(service).findAll(Pageable.unpaged());
    }

    @Test
    void findMineShouldUseAuthenticatedStudent() {
        WorkoutSheetService service = mock(WorkoutSheetService.class);
        WorkoutSheetController controller = new WorkoutSheetController(service);
        Authentication authentication = authentication("ana@gymiq.com", "ROLE_STUDENT");

        when(service.findByAuthenticatedStudent("ana@gymiq.com", true, Pageable.unpaged()))
                .thenReturn(Page.empty());

        ResponseEntity<Page<WorkoutSheetSummaryResponse>> response = controller.findMine(
                authentication,
                true,
                Pageable.unpaged());

        assertThat(response.getBody()).isNotNull();
        verify(service).findByAuthenticatedStudent("ana@gymiq.com", true, Pageable.unpaged());
    }

    @Test
    void findByIdShouldPassRoleFlagsToService() {
        WorkoutSheetService service = mock(WorkoutSheetService.class);
        WorkoutSheetController controller = new WorkoutSheetController(service);
        WorkoutSheet sheet = TestDataFactory.workoutSheet();
        Authentication authentication = authentication("carlos@gymiq.com", "ROLE_INSTRUCTOR");

        when(service.findById(sheet.getWorkoutSheetId(), "carlos@gymiq.com", false, true, false))
                .thenReturn(WorkoutSheetResponse.fromEntity(sheet));

        ResponseEntity<WorkoutSheetResponse> response = controller.findById(sheet.getWorkoutSheetId(), authentication);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Ficha Hipertrofia");
        verify(service).findById(sheet.getWorkoutSheetId(), "carlos@gymiq.com", false, true, false);
    }

    @Test
    void findByStudentShouldDelegateWithAdminFlag() {
        WorkoutSheetService service = mock(WorkoutSheetService.class);
        WorkoutSheetController controller = new WorkoutSheetController(service);
        WorkoutSheet sheet = TestDataFactory.workoutSheet();
        Authentication authentication = authentication("admin@gymiq.com", "ROLE_ADMIN");

        when(service.findByStudent(sheet.getStudent().getStudentId(), true, Pageable.unpaged(), "admin@gymiq.com", true))
                .thenReturn(Page.empty());

        ResponseEntity<Page<WorkoutSheetSummaryResponse>> response = controller.findByStudent(
                sheet.getStudent().getStudentId(),
                authentication,
                true,
                Pageable.unpaged());

        assertThat(response.getBody()).isNotNull();
        verify(service).findByStudent(sheet.getStudent().getStudentId(), true, Pageable.unpaged(), "admin@gymiq.com", true);
    }

    @Test
    void updateShouldReturnUpdatedWorkoutSheet() {
        WorkoutSheetService service = mock(WorkoutSheetService.class);
        WorkoutSheetController controller = new WorkoutSheetController(service);
        WorkoutSheet sheet = TestDataFactory.workoutSheet();
        CreateWorkoutSheetRequest request = request(sheet);
        Authentication authentication = authentication("admin@gymiq.com", "ROLE_ADMIN");

        when(service.update(sheet.getWorkoutSheetId(), request, "admin@gymiq.com", true))
                .thenReturn(WorkoutSheetResponse.fromEntity(sheet));

        ResponseEntity<WorkoutSheetResponse> response = controller.update(sheet.getWorkoutSheetId(), authentication, request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getGoal()).isEqualTo("Ganho de massa");
        verify(service).update(sheet.getWorkoutSheetId(), request, "admin@gymiq.com", true);
    }

    @Test
    void deactivateShouldReturnNoContent() {
        WorkoutSheetService service = mock(WorkoutSheetService.class);
        WorkoutSheetController controller = new WorkoutSheetController(service);
        WorkoutSheet sheet = TestDataFactory.workoutSheet();
        Authentication authentication = authentication("carlos@gymiq.com", "ROLE_INSTRUCTOR");

        ResponseEntity<Void> response = controller.deactivate(sheet.getWorkoutSheetId(), authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(service).deactivate(sheet.getWorkoutSheetId(), "carlos@gymiq.com", false);
    }

    @Test
    void deleteShouldReturnNoContent() {
        WorkoutSheetService service = mock(WorkoutSheetService.class);
        WorkoutSheetController controller = new WorkoutSheetController(service);
        WorkoutSheet sheet = TestDataFactory.workoutSheet();
        Authentication authentication = authentication("admin@gymiq.com", "ROLE_ADMIN");

        ResponseEntity<Void> response = controller.delete(sheet.getWorkoutSheetId(), authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(service).delete(sheet.getWorkoutSheetId(), "admin@gymiq.com", true);
    }

    private CreateWorkoutSheetRequest request(WorkoutSheet sheet) {
        CreateWorkoutSheetRequest request = new CreateWorkoutSheetRequest();
        request.setStudentId(sheet.getStudent().getStudentId());
        request.setInstructorId(sheet.getInstructor().getInstructorId());
        request.setName("Ficha Hipertrofia");
        request.setGoal("Ganho de massa");
        request.setStartDate(LocalDate.of(2026, 5, 1));
        request.setEndDate(LocalDate.of(2026, 8, 1));
        request.setNotes("Ajustar cargas semanalmente");
        return request;
    }

    private Authentication authentication(String email, String role) {
        return new TestingAuthenticationToken(email, "n/a", role);
    }
}
