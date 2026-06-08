package com.gymiq.service;

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
import java.util.UUID;

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

    @Mock
    private PersonalDataProtectionService personalDataProtectionService;

    @Mock
    private StudentContractService studentContractService;

    @InjectMocks
    private PresenceService presenceService;

    @Test
    void checkInShouldCreatePresenceWhenDailyLimitWasNotReached() {
        Student student = TestDataFactory.activeStudent();
        LocalDateTime checkInAt = LocalDateTime.of(2026, 5, 15, 9, 0);
        CreatePresenceRequest request = new CreatePresenceRequest();
        request.setStudentId(student.getStudentId());
        request.setCheckInAt(checkInAt);
        request.setNotes("Musculacao");

        when(studentRepository.findById(student.getStudentId())).thenReturn(Optional.of(student));
        when(presenceRepository.countByStudentStudentIdAndCheckInAtGreaterThanEqualAndCheckInAtLessThan(
                student.getStudentId(),
                checkInAt.toLocalDate().atStartOfDay(),
                checkInAt.toLocalDate().plusDays(1).atStartOfDay()))
                .thenReturn(3L);
        when(presenceRepository.save(any(Presence.class))).thenAnswer(invocation -> {
            Presence presence = invocation.getArgument(0);
            presence.setPresenceId(UUID.fromString("00000000-0000-0000-0000-000000000005"));
            return presence;
        });

        PresenceResponse response = presenceService.checkIn(request);

        assertThat(response.getPresenceId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000005"));
        assertThat(response.getStudentId()).isEqualTo(student.getStudentId());
        assertThat(response.getCheckInAt()).isEqualTo(checkInAt);
        verify(studentContractService).validateStudentCheckInAccess(student.getStudentId());
        verify(presenceRepository).save(any(Presence.class));
    }

    @Test
    void checkInShouldRejectWhenDailyLimitWasReached() {
        Student student = TestDataFactory.activeStudent();
        LocalDateTime checkInAt = LocalDateTime.of(2026, 5, 15, 9, 0);
        CreatePresenceRequest request = new CreatePresenceRequest();
        request.setStudentId(student.getStudentId());
        request.setCheckInAt(checkInAt);

        when(studentRepository.findById(student.getStudentId())).thenReturn(Optional.of(student));
        when(presenceRepository.countByStudentStudentIdAndCheckInAtGreaterThanEqualAndCheckInAtLessThan(
                student.getStudentId(),
                checkInAt.toLocalDate().atStartOfDay(),
                checkInAt.toLocalDate().plusDays(1).atStartOfDay()))
                .thenReturn(4L);

        assertThatThrownBy(() -> presenceService.checkIn(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("limite");
    }
}
