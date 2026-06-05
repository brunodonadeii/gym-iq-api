package com.gymiq.service;

import com.gymiq.aop.Auditable;
import com.gymiq.dto.request.CreateStudentRequest;
import com.gymiq.dto.request.UpdateStudentRequest;
import com.gymiq.dto.response.StudentOptionResponse;
import com.gymiq.dto.response.StudentResponse;
import com.gymiq.dto.response.StudentSummaryResponse;
import com.gymiq.entity.Enrollment;
import com.gymiq.entity.Enrollment.EnrollmentStatus;
import com.gymiq.entity.Payment.PaymentStatus;
import com.gymiq.entity.Student;
import com.gymiq.entity.User;
import com.gymiq.enums.AuditAction;
import com.gymiq.enums.ResourceType;
import com.gymiq.exception.BusinessException;
import com.gymiq.exception.ResourceNotFoundException;
import com.gymiq.repository.EnrollmentRepository;
import com.gymiq.repository.PaymentRepository;
import com.gymiq.repository.StudentRepository;
import com.gymiq.repository.UserRepository;
import com.gymiq.security.PersonalDataExposurePolicy;
import com.gymiq.security.PersonalDataProtection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {

    public enum StudentListStatus {
        ACTIVE,
        INACTIVE,
        ALL
    }

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PaymentRepository paymentRepository;
    private final PasswordEncoder passwordEncoder;
    private final StudentDataService studentDataService;
    private final PersonalDataProtectionService personalDataProtectionService;

    @Transactional
    @Auditable(action = AuditAction.CREATE_STUDENT, resourceType = ResourceType.STUDENT, description = "Criou aluno")
    public StudentResponse create(CreateStudentRequest request) {
        studentDataService.validateCpf(request.getCpf());

        String emailHash = personalDataProtectionService.emailHash(request.getEmail());
        String cpfHash = personalDataProtectionService.cpfHash(request.getCpf());

        if (userRepository.existsByEmailHash(emailHash)) {
            throw new BusinessException("E-mail ja cadastrado: " + request.getEmail());
        }
        if (studentRepository.existsByCpfHash(cpfHash)) {
            throw new BusinessException("CPF ja cadastrado: " + request.getCpf());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .emailHash(emailHash)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.STUDENT)
                .active(true)
                .lgpdAccepted(request.getLgpdAccepted())
                .lgpdAcceptedAt(resolveLgpdAcceptedAt(request.getLgpdAccepted()))
                .build();
        userRepository.save(user);

        Student student = Student.builder()
                .user(user)
                .cpf(request.getCpf())
                .cpfHash(cpfHash)
                .birthDate(request.getBirthDate())
                .phone(request.getPhone())
                .zipCode(request.getZipCode())
                .address(studentDataService.resolveAddress(request.getZipCode(), request.getAddress()))
                .build();
        studentRepository.save(student);

        log.info("Student created: id={}, name={}", student.getStudentId(), user.getName());
        return StudentResponse.fromEntity(student);
    }

    @Transactional(readOnly = true)
    public Page<StudentSummaryResponse> findAll(Pageable pageable) {
        return findAll(StudentListStatus.ACTIVE, false, pageable);
    }

    @Transactional(readOnly = true)
    public Page<StudentSummaryResponse> findAll(StudentListStatus status, boolean admin, Pageable pageable) {
        StudentListStatus resolvedStatus = status != null ? status : StudentListStatus.ACTIVE;

        if (resolvedStatus != StudentListStatus.ACTIVE && !admin) {
            throw new AccessDeniedException("Apenas administradores podem consultar alunos inativos ou anonimizados");
        }

        Page<Student> students = switch (resolvedStatus) {
            case ACTIVE -> studentRepository.findByUserActiveTrue(pageable);
            case INACTIVE -> studentRepository.findByUserActiveFalse(pageable);
            case ALL -> studentRepository.findAll(pageable);
        };

        return students.map(StudentSummaryResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<StudentSummaryResponse> search(String term, Pageable pageable) {
        return studentRepository.searchByTerm(
                        term,
                        resolveEmailHashForSearch(term),
                        resolveCpfHashForSearch(term),
                        pageable)
                .map(StudentSummaryResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<StudentOptionResponse> findOptions(String term) {
        List<StudentOptionResponse> options = studentRepository.findOptions(
                term,
                resolveEmailHashForSearch(term),
                resolveCpfHashForSearch(term),
                PageRequest.of(0, 20));

        if (PersonalDataExposurePolicy.canViewFullStudentData()) {
            return options;
        }

        return options.stream()
                .map(option -> new StudentOptionResponse(
                        option.getStudentId(),
                        option.getName(),
                        PersonalDataProtection.maskEmail(option.getEmail()),
                        PersonalDataProtection.maskCpf(option.getCpf()),
                        option.getName() + " - " + PersonalDataProtection.maskCpf(option.getCpf())))
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentResponse findById(UUID id) {
        return StudentResponse.fromEntity(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public StudentResponse findByAuthenticatedEmail(String email) {
        return StudentResponse.fromEntity(findEntityByAuthenticatedEmail(email));
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE_STUDENT, resourceType = ResourceType.STUDENT, description = "Atualizou dados do aluno")
    public StudentResponse update(UUID id, UpdateStudentRequest request) {
        Student student = findEntityById(id);
        User user = student.getUser();

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String emailHash = personalDataProtectionService.emailHash(request.getEmail());
            userRepository.findByEmailHash(emailHash)
                    .filter(existingUser -> !existingUser.getUserId().equals(user.getUserId()))
                    .ifPresent(existingUser -> {
                        throw new BusinessException("E-mail ja usado por outro usuario");
                    });
            user.setEmail(request.getEmail());
            user.setEmailHash(emailHash);
        }

        if (request.getCpf() != null && !request.getCpf().isBlank()) {
            studentDataService.validateCpf(request.getCpf());
            String cpfHash = personalDataProtectionService.cpfHash(request.getCpf());
            studentRepository.findByCpfHash(cpfHash)
                    .filter(existingStudent -> !existingStudent.getStudentId().equals(id))
                    .ifPresent(existingStudent -> {
                        throw new BusinessException("CPF ja usado por outro aluno");
                    });
            student.setCpf(request.getCpf());
            student.setCpfHash(cpfHash);
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }
        if (request.getBirthDate() != null) {
            student.setBirthDate(request.getBirthDate());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            student.setPhone(request.getPhone());
        }
        if (request.getZipCode() != null) {
            student.setZipCode(request.getZipCode());
        }
        if (request.getAddress() != null || request.getZipCode() != null) {
            student.setAddress(studentDataService.resolveAddress(request.getZipCode(), request.getAddress()));
        }

        studentRepository.save(student);
        log.info("Student updated: id={}", id);
        return StudentResponse.fromEntity(student);
    }

    @Transactional
    @Auditable(action = AuditAction.DEACTIVATE_STUDENT, resourceType = ResourceType.STUDENT, description = "Inativou aluno")
    public void deactivate(UUID id) {
        Student student = findEntityById(id);
        cancelActiveEnrollmentIfPresent(student);
        student.getUser().setActive(false);
        studentRepository.save(student);
        log.info("Student deactivated: id={}", id);
    }

    @Transactional
    @Auditable(action = AuditAction.ACTIVATE_STUDENT, resourceType = ResourceType.STUDENT, description = "Ativou aluno")
    public StudentResponse activate(UUID id) {
        Student student = findEntityById(id);
        student.getUser().setActive(true);
        studentRepository.save(student);
        log.info("Student activated: id={}", id);
        return StudentResponse.fromEntity(student);
    }

    @Transactional
    @Auditable(action = AuditAction.ANONYMIZE_STUDENT, resourceType = ResourceType.STUDENT, description = "Anonimizou aluno")
    public StudentResponse anonymize(UUID id) {
        Student student = findEntityById(id);
        return anonymizeStudentData(student);
    }

    @Transactional
    @Auditable(action = AuditAction.ANONYMIZE_STUDENT, resourceType = ResourceType.STUDENT, description = "Anonimizou seus proprios dados")
    public StudentResponse anonymizeAuthenticatedStudent(String email) {
        Student student = findEntityByAuthenticatedEmail(email);
        return anonymizeStudentData(student);
    }

    private StudentResponse anonymizeStudentData(Student student) {
        User user = student.getUser();

        validateDataDeletionEligibility(student);

        String anonymizedEmail = UUID.randomUUID() + "@deleted.gymiq.com";
        String anonymizedCpf = buildAnonymizedCpf(student);

        user.setName("Usuário Anonimizado");
        user.setEmail(anonymizedEmail);
        user.setEmailHash(personalDataProtectionService.emailHash(anonymizedEmail));
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setActive(false);

        student.setCpf(anonymizedCpf);
        student.setCpfHash(personalDataProtectionService.cpfHash(anonymizedCpf));
        student.setBirthDate(LocalDate.of(1900, 1, 1));
        student.setPhone("00000000000");
        student.setZipCode(null);
        student.setAddress(null);

        studentRepository.save(student);
        log.info("Student anonymized: id={}", student.getStudentId());
        return StudentResponse.fromEntity(student);
    }

    public Student findEntityById(UUID id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno nao encontrado: " + id));
    }

    public Student findEntityByAuthenticatedEmail(String email) {
        return studentRepository.findByUserEmailHash(personalDataProtectionService.emailHash(email))
                .orElseThrow(() -> new ResourceNotFoundException("Aluno nao encontrado para o usuario autenticado"));
    }

    private LocalDateTime resolveLgpdAcceptedAt(Boolean lgpdAccepted) {
        return Boolean.TRUE.equals(lgpdAccepted) ? LocalDateTime.now() : null;
    }

    private void cancelActiveEnrollmentIfPresent(Student student) {
        enrollmentRepository.findByStudentStudentIdAndStatus(student.getStudentId(), EnrollmentStatus.ACTIVE)
                .ifPresent(enrollment -> {
                    enrollment.setStatus(EnrollmentStatus.CANCELED);
                    enrollment.setCanceledAt(LocalDateTime.now());
                    enrollmentRepository.save(enrollment);
                    log.info("Active enrollment canceled during student deactivation/anonymization: enrollmentId={}, studentId={}",
                            enrollment.getEnrollmentId(), student.getStudentId());
                });
    }

    private void validateDataDeletionEligibility(Student student) {
        enrollmentRepository.findTopByStudentStudentIdOrderByStartDateDescCreatedAtDesc(student.getStudentId())
                .map(Enrollment::getStatus)
                .filter(EnrollmentStatus.ACTIVE::equals)
                .ifPresent(status -> {
                    throw new BusinessException(
                            "Não é possível excluir os dados. O aluno possui uma matrícula ativa. Solicite o cancelamento primeiro.");
                });

        if (paymentRepository.existsByEnrollmentStudentStudentIdAndStatusIn(
                student.getStudentId(),
                List.of(PaymentStatus.PENDING, PaymentStatus.OVERDUE))) {
            throw new BusinessException(
                    "Não é possível excluir os dados. O aluno possui pendências financeiras em aberto.");
        }
    }

    private String buildAnonymizedCpf(Student student) {
        String digits = UUID.randomUUID()
                .toString()
                .replaceAll("\\D", "");

        while (digits.length() < 11) {
            digits += UUID.randomUUID().toString().replaceAll("\\D", "");
        }

        digits = digits.substring(0, 11);
        return digits.substring(0, 3) + "." +
                digits.substring(3, 6) + "." +
                digits.substring(6, 9) + "-" +
                digits.substring(9, 11);
    }

    private String resolveEmailHashForSearch(String term) {
        return term != null && term.contains("@")
                ? personalDataProtectionService.emailHash(term)
                : null;
    }

    private String resolveCpfHashForSearch(String term) {
        String digits = term == null ? "" : term.replaceAll("\\D", "");
        return digits.length() == 11 ? personalDataProtectionService.cpfHash(term) : null;
    }
}
