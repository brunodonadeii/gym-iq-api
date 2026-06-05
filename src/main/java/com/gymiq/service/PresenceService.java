package com.gymiq.service;

import java.util.UUID;

import com.gymiq.aop.Auditable;
import com.gymiq.dto.request.CreatePresenceRequest;
import com.gymiq.dto.request.SelfCheckInRequest;
import com.gymiq.dto.response.PresenceResponse;
import com.gymiq.entity.Presence;
import com.gymiq.entity.Student;
import com.gymiq.enums.AuditAction;
import com.gymiq.enums.ResourceType;
import com.gymiq.exception.BusinessException;
import com.gymiq.exception.ResourceNotFoundException;
import com.gymiq.repository.PresenceRepository;
import com.gymiq.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class PresenceService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final int MAX_DAILY_CHECK_INS = 4;

    private final PresenceRepository presenceRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final PersonalDataProtectionService personalDataProtectionService;

    @Transactional
    @Auditable(action = AuditAction.CHECK_IN, resourceType = ResourceType.PRESENCE, description = "Registrou check-in")
    public PresenceResponse checkIn(CreatePresenceRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Aluno nao encontrado: " + request.getStudentId()));

        return createPresence(student, request.getCheckInAt(), request.getNotes());
    }

    @Transactional
    @Auditable(action = AuditAction.SELF_CHECK_IN, resourceType = ResourceType.PRESENCE, description = "Registrou auto check-in")
    public PresenceResponse selfCheckIn(SelfCheckInRequest request) {
        String identifier = request.getIdentifier().trim();
        String cpfHash = resolveCpfHash(identifier);
        String emailHash = resolveEmailHash(identifier);

        Student student = studentRepository
                .findByCpfHashOrUserEmailHash(cpfHash, emailHash)
                .orElseThrow(() -> new BusinessException("Identificador ou senha invalidos"));

        if (!passwordEncoder.matches(request.getPassword(), student.getUser().getPasswordHash())) {
            throw new BusinessException("Identificador ou senha invalidos");
        }

        return createPresence(student, LocalDateTime.now(BUSINESS_ZONE), request.getNotes());
    }

    private PresenceResponse createPresence(Student student, LocalDateTime requestedCheckInAt, String notes) {
        if (Boolean.FALSE.equals(student.getUser().getActive())) {
            throw new BusinessException("Nao e possivel registrar presenca para aluno inativo");
        }

        LocalDateTime checkInAt = requestedCheckInAt != null
                ? requestedCheckInAt
                : LocalDateTime.now(BUSINESS_ZONE);

        validateDailyCheckInLimit(student.getStudentId(), checkInAt.toLocalDate());

        Presence presence = Presence.builder()
                .student(student)
                .checkInAt(checkInAt)
                .notes(notes)
                .build();

        presenceRepository.save(presence);
        log.info("Presence check-in created: id={}, student={}", presence.getPresenceId(), student.getStudentId());
        return PresenceResponse.fromEntity(presence);
    }

    private void validateDailyCheckInLimit(UUID studentId, LocalDate checkInDate) {
        LocalDateTime startOfDay = checkInDate.atStartOfDay();
        LocalDateTime startOfNextDay = checkInDate.plusDays(1).atStartOfDay();

        long dailyCheckIns = presenceRepository.countByStudentStudentIdAndCheckInAtGreaterThanEqualAndCheckInAtLessThan(
                studentId,
                startOfDay,
                startOfNextDay);

        if (dailyCheckIns >= MAX_DAILY_CHECK_INS) {
            throw new BusinessException("Acesso negado. O limite maximo de 4 check-ins diarios foi atingido.");
        }
    }

    @Transactional(readOnly = true)
    public Page<PresenceResponse> findAll(Pageable pageable) {
        return presenceRepository.findAll(pageable)
                .map(PresenceResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public PresenceResponse findById(UUID id) {
        return PresenceResponse.fromEntity(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public Page<PresenceResponse> findByStudent(UUID studentId, Pageable pageable) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Aluno nao encontrado: " + studentId);
        }

        return presenceRepository.findByStudentStudentId(studentId, pageable)
                .map(PresenceResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<PresenceResponse> findByAuthenticatedStudent(String email, Pageable pageable) {
        UUID studentId = studentRepository.findByUserEmailHash(personalDataProtectionService.emailHash(email))
                .orElseThrow(() -> new ResourceNotFoundException("Aluno nao encontrado para o usuario autenticado"))
                .getStudentId();

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

    private Presence findEntityById(UUID id) {
        return presenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Presenca nao encontrada: " + id));
    }

    private String resolveCpfHash(String identifier) {
        String digits = identifier == null ? "" : identifier.replaceAll("\\D", "");
        return digits.length() == 11 ? personalDataProtectionService.cpfHash(identifier) : null;
    }

    private String resolveEmailHash(String identifier) {
        return identifier != null && identifier.contains("@")
                ? personalDataProtectionService.emailHash(identifier)
                : null;
    }
}
