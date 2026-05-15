package com.gymiq.service;

import com.gymiq.dto.request.CheckOutPresenceRequest;
import com.gymiq.dto.request.CreatePresenceRequest;
import com.gymiq.dto.response.PresenceResponse;
import com.gymiq.entity.Presence;
import com.gymiq.entity.Student;
import com.gymiq.exception.BusinessException;
import com.gymiq.repository.PresenceRepository;
import com.gymiq.repository.StudentRepository;
import com.gymiq.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresenceServiceTest {

    @Mock
    private PresenceRepository presenceRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private PresenceService presenceService;

    @Test
    void checkInShouldCreateOpenPresence() {
        Student student = TestDataFactory.activeStudent();
        CreatePresenceRequest request = new CreatePresenceRequest();
        request.setStudentId(student.getStudentId());
        request.setCheckInAt(LocalDateTime.of(2026, 5, 15, 9, 0));
        request.setNotes("Musculacao");

        when(studentRepository.findById(student.getStudentId())).thenReturn(Optional.of(student));
        when(presenceRepository.findByStudentStudentIdAndCheckOutAtIsNull(student.getStudentId()))
                .thenReturn(Optional.empty());
        when(presenceRepository.save(any(Presence.class))).thenAnswer(invocation -> {
            Presence presence = invocation.getArgument(0);
            presence.setPresenceId(5);
            return presence;
        });

        PresenceResponse response = presenceService.checkIn(request);

        assertThat(response.getPresenceId()).isEqualTo(5);
        assertThat(response.getStudentId()).isEqualTo(student.getStudentId());
        assertThat(response.getCheckOutAt()).isNull();
    }

    @Test
    void checkInShouldRejectStudentWithOpenPresence() {
        Student student = TestDataFactory.activeStudent();
        CreatePresenceRequest request = new CreatePresenceRequest();
        request.setStudentId(student.getStudentId());

        when(studentRepository.findById(student.getStudentId())).thenReturn(Optional.of(student));
        when(presenceRepository.findByStudentStudentIdAndCheckOutAtIsNull(student.getStudentId()))
                .thenReturn(Optional.of(TestDataFactory.openPresence()));

        assertThatThrownBy(() -> presenceService.checkIn(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("check-in aberto");
    }

    @Test
    void checkOutShouldRejectDateBeforeCheckIn() {
        Presence presence = TestDataFactory.openPresence();
        CheckOutPresenceRequest request = new CheckOutPresenceRequest();
        request.setCheckOutAt(presence.getCheckInAt().minusMinutes(1));

        when(presenceRepository.findById(presence.getPresenceId())).thenReturn(Optional.of(presence));

        assertThatThrownBy(() -> presenceService.checkOut(presence.getPresenceId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("anterior");
    }

    @Test
    void checkOutShouldClosePresence() {
        Presence presence = TestDataFactory.openPresence();
        CheckOutPresenceRequest request = new CheckOutPresenceRequest();
        request.setCheckOutAt(presence.getCheckInAt().plusHours(1));
        request.setNotes("Treino concluido");

        when(presenceRepository.findById(presence.getPresenceId())).thenReturn(Optional.of(presence));

        PresenceResponse response = presenceService.checkOut(presence.getPresenceId(), request);

        assertThat(response.getCheckOutAt()).isEqualTo(request.getCheckOutAt());
        assertThat(response.getNotes()).isEqualTo("Treino concluido");
        verify(presenceRepository).save(presence);
    }
}
