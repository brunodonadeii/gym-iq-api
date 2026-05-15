package com.gymiq.service;

import com.gymiq.dto.request.CheckOutPresenceRequest;
import com.gymiq.dto.request.CreatePresenceRequest;
import com.gymiq.dto.response.PresenceResponse;
import com.gymiq.entity.Presence;
import com.gymiq.entity.Student;
import com.gymiq.exception.BusinessException;
import com.gymiq.exception.ResourceNotFoundException;
import com.gymiq.repository.PresenceRepository;
import com.gymiq.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PresenceService {

    private final PresenceRepository presenceRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public PresenceResponse checkIn(CreatePresenceRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Aluno nao encontrado: " + request.getStudentId()));

        if (Boolean.FALSE.equals(student.getUser().getActive())) {
            throw new BusinessException("Nao e possivel registrar presenca para aluno inativo");
        }

        presenceRepository.findByStudentStudentIdAndCheckOutAtIsNull(student.getStudentId())
                .ifPresent(p -> { throw new BusinessException("Aluno ja possui check-in aberto"); });

        LocalDateTime checkInAt = request.getCheckInAt() != null
                ? request.getCheckInAt()
                : LocalDateTime.now();

        Presence presence = Presence.builder()
                .student(student)
                .checkInAt(checkInAt)
                .notes(request.getNotes())
                .build();

        presenceRepository.save(presence);
        log.info("Presence check-in created: id={}, student={}", presence.getPresenceId(), student.getStudentId());
        return PresenceResponse.fromEntity(presence);
    }

    @Transactional
    public PresenceResponse checkOut(Integer id, CheckOutPresenceRequest request) {
        Presence presence = findEntityById(id);
        CheckOutPresenceRequest checkOutRequest = request != null ? request : new CheckOutPresenceRequest();

        if (presence.getCheckOutAt() != null) {
            throw new BusinessException("Presenca ja possui check-out registrado");
        }

        LocalDateTime checkOutAt = checkOutRequest.getCheckOutAt() != null
                ? checkOutRequest.getCheckOutAt()
                : LocalDateTime.now();

        if (checkOutAt.isBefore(presence.getCheckInAt())) {
            throw new BusinessException("Check-out nao pode ser anterior ao check-in");
        }

        presence.setCheckOutAt(checkOutAt);
        if (checkOutRequest.getNotes() != null) {
            presence.setNotes(checkOutRequest.getNotes());
        }

        presenceRepository.save(presence);
        log.info("Presence check-out registered: id={}, checkOutAt={}", id, presence.getCheckOutAt());
        return PresenceResponse.fromEntity(presence);
    }

    @Transactional(readOnly = true)
    public Page<PresenceResponse> findAll(Pageable pageable) {
        return presenceRepository.findAll(pageable)
                .map(PresenceResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public PresenceResponse findById(Integer id) {
        return PresenceResponse.fromEntity(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public Page<PresenceResponse> findByStudent(Integer studentId, Pageable pageable) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Aluno nao encontrado: " + studentId);
        }

        return presenceRepository.findByStudentStudentId(studentId, pageable)
                .map(PresenceResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<PresenceResponse> findByDate(LocalDate date, Pageable pageable) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay().minusNanos(1);

        return presenceRepository.findByCheckInAtBetween(start, end, pageable)
                .map(PresenceResponse::fromEntity);
    }

    private Presence findEntityById(Integer id) {
        return presenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Presenca nao encontrada: " + id));
    }
}
