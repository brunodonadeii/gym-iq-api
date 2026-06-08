package com.gymiq.service;

import com.gymiq.dto.request.CreateInstructorRequest;
import com.gymiq.dto.request.UpdateInstructorRequest;
import com.gymiq.dto.response.InstructorResponse;
import com.gymiq.entity.Instructor;
import com.gymiq.entity.User;
import com.gymiq.exception.BusinessException;
import com.gymiq.repository.InstructorRepository;
import com.gymiq.repository.UserRepository;
import com.gymiq.repository.WorkoutSheetRepository;
import com.gymiq.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class InstructorServiceTest {

    @Mock
    private InstructorRepository instructorRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkoutSheetRepository workoutSheetRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PersonalDataProtectionService personalDataProtectionService;

    @InjectMocks
    private InstructorService instructorService;

    @Test
    void createShouldPersistInstructorUserAndProfile() {
        CreateInstructorRequest request = validCreateRequest();

        when(personalDataProtectionService.emailHash(request.getEmail())).thenReturn("email-hash");
        when(userRepository.existsByEmailHash("email-hash")).thenReturn(false);
        when(instructorRepository.existsByCref(request.getCref())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000020"));
            return user;
        });
        when(instructorRepository.save(any(Instructor.class))).thenAnswer(invocation -> {
            Instructor instructor = invocation.getArgument(0);
            instructor.setInstructorId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
            return instructor;
        });

        InstructorResponse response = instructorService.create(request);

        assertThat(response.getInstructorId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        assertThat(response.getName()).isEqualTo("Carlos Trainer");
        assertThat(response.getSpecialty()).isEqualTo("Musculacao");
        verify(userRepository).save(any(User.class));
        verify(instructorRepository).save(any(Instructor.class));
    }

    @Test
    void updateShouldChangeInstructorAndLgpdAcceptance() {
        Instructor instructor = TestDataFactory.activeInstructor();
        UpdateInstructorRequest request = validUpdateRequest();

        when(instructorRepository.findById(instructor.getInstructorId())).thenReturn(Optional.of(instructor));
        when(personalDataProtectionService.emailHash(request.getEmail())).thenReturn("new-email-hash");
        when(userRepository.findByEmailHash("new-email-hash")).thenReturn(Optional.empty());
        when(instructorRepository.findByCref(request.getCref())).thenReturn(Optional.empty());

        InstructorResponse response = instructorService.update(instructor.getInstructorId(), request);

        assertThat(response.getName()).isEqualTo("Carlos Atualizado");
        assertThat(instructor.getUser().getEmailHash()).isEqualTo("new-email-hash");
        assertThat(instructor.getSpecialty()).isEqualTo("Funcional");
        verify(instructorRepository).save(instructor);
    }

    @Test
    void deleteShouldRejectInstructorLinkedToWorkoutSheets() {
        Instructor instructor = TestDataFactory.activeInstructor();

        when(instructorRepository.findById(instructor.getInstructorId())).thenReturn(Optional.of(instructor));
        when(workoutSheetRepository.existsByInstructorInstructorId(instructor.getInstructorId())).thenReturn(true);

        assertThatThrownBy(() -> instructorService.delete(instructor.getInstructorId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("fichas");
    }

    @Test
    void deactivateAndActivateShouldToggleUserStatus() {
        Instructor instructor = TestDataFactory.activeInstructor();

        when(instructorRepository.findById(instructor.getInstructorId())).thenReturn(Optional.of(instructor));

        instructorService.deactivate(instructor.getInstructorId());
        assertThat(instructor.getUser().getActive()).isFalse();

        instructorService.activate(instructor.getInstructorId());
        assertThat(instructor.getUser().getActive()).isTrue();

        verify(instructorRepository, times(2)).save(instructor);
    }

    private CreateInstructorRequest validCreateRequest() {
        CreateInstructorRequest request = new CreateInstructorRequest();
        request.setName("Carlos Trainer");
        request.setEmail("carlos@gymiq.com");
        request.setPassword("secret123");
        request.setCref("123456-G/SP");
        request.setPhone("11988887777");
        request.setSpecialty("Musculacao");
        request.setLgpdAccepted(true);
        return request;
    }

    private UpdateInstructorRequest validUpdateRequest() {
        UpdateInstructorRequest request = new UpdateInstructorRequest();
        request.setName("Carlos Atualizado");
        request.setEmail("carlos.novo@gymiq.com");
        request.setCref("654321-G/SP");
        request.setPhone("11977776666");
        request.setSpecialty("Funcional");
        request.setLgpdAccepted(true);
        return request;
    }
}
