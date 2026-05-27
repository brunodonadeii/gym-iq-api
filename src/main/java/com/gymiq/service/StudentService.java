package com.gymiq.service;

import com.gymiq.dto.request.CreateStudentRequest;
import com.gymiq.dto.request.UpdateStudentRequest;
import com.gymiq.dto.response.StudentOptionResponse;
import com.gymiq.dto.response.StudentResponse;
import com.gymiq.entity.Enrollment.EnrollmentStatus;
import com.gymiq.entity.Student;
import com.gymiq.entity.User;
import com.gymiq.exception.BusinessException;
import com.gymiq.exception.ResourceNotFoundException;
import com.gymiq.repository.EnrollmentRepository;
import com.gymiq.repository.StudentRepository;
import com.gymiq.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final StudentDataService studentDataService;

    @Transactional
    public StudentResponse create(CreateStudentRequest request) {
        studentDataService.validateCpf(request.getCpf());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("E-mail já cadastrado: " + request.getEmail());
        }
        if (studentRepository.existsByCpf(request.getCpf())) {
            throw new BusinessException("CPF já cadastrado: " + request.getCpf());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
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
    public Page<StudentResponse> findAll(Pageable pageable) {
        return studentRepository.findAll(pageable)
                .map(StudentResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<StudentResponse> search(String term, Pageable pageable) {
        return studentRepository.searchByTerm(term, pageable)
                .map(StudentResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<StudentOptionResponse> findOptions(String term) {
        return studentRepository.findOptions(term, PageRequest.of(0, 20));
    }

    @Transactional(readOnly = true)
    public StudentResponse findById(Integer id) {
        return StudentResponse.fromEntity(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public StudentResponse findByAuthenticatedEmail(String email) {
        return StudentResponse.fromEntity(findEntityByAuthenticatedEmail(email));
    }

    @Transactional
    public StudentResponse update(Integer id, UpdateStudentRequest request) {
        Student student = findEntityById(id);
        User user = student.getUser();

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            userRepository.findByEmail(request.getEmail())
                    .filter(u -> !u.getUserId().equals(user.getUserId()))
                    .ifPresent(u -> { throw new BusinessException("E-mail já usado por outro usuário"); });
            user.setEmail(request.getEmail());
        }

        if (request.getCpf() != null && !request.getCpf().isBlank()) {
            studentDataService.validateCpf(request.getCpf());
            studentRepository.findByCpf(request.getCpf())
                    .filter(s -> !s.getStudentId().equals(id))
                    .ifPresent(s -> { throw new BusinessException("CPF já usado por outro aluno"); });
            student.setCpf(request.getCpf());
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
    public void deactivate(Integer id) {
        Student student = findEntityById(id);
        cancelActiveEnrollmentIfPresent(student);
        student.getUser().setActive(false);
        studentRepository.save(student);
        log.info("Student deactivated: id={}", id);
    }

    @Transactional
    public StudentResponse anonymize(Integer id) {
        Student student = findEntityById(id);
        User user = student.getUser();

        if (Boolean.TRUE.equals(user.getActive())) {
            throw new BusinessException("Aluno precisa estar inativo antes da anonimização");
        }

        cancelActiveEnrollmentIfPresent(student);

        user.setName("Aluno anonimizado #" + student.getStudentId());
        user.setEmail(buildAnonymizedEmail(student));
        user.setPasswordHash("{anonymized}");

        student.setCpf(buildAnonymizedCpf(student));
        student.setBirthDate(LocalDate.of(1900, 1, 1));
        student.setPhone("ANONYMIZED");
        student.setZipCode(null);
        student.setAddress(null);

        studentRepository.save(student);
        log.info("Student anonymized: id={}", id);
        return StudentResponse.fromEntity(student);
    }

    public Student findEntityById(Integer id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado: " + id));
    }
    public Student findEntityByAuthenticatedEmail(String email) {
        return studentRepository.findByUserEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno nao encontrado para o usuario autenticado"));
    }

    private LocalDateTime resolveLgpdAcceptedAt(Boolean lgpdAccepted) {
        return Boolean.TRUE.equals(lgpdAccepted) ? LocalDateTime.now() : null;
    }

    private void cancelActiveEnrollmentIfPresent(Student student) {
        enrollmentRepository.findByStudentStudentIdAndStatus(student.getStudentId(), EnrollmentStatus.ACTIVE)
                .ifPresent(enrollment -> {
                    enrollment.setStatus(EnrollmentStatus.CANCELED);
                    enrollmentRepository.save(enrollment);
                    log.info("Active enrollment canceled during student deactivation/anonymization: enrollmentId={}, studentId={}",
                            enrollment.getEnrollmentId(), student.getStudentId());
                });
    }

    private String buildAnonymizedEmail(Student student) {
        return "anonymized.student." + student.getStudentId() + "." + student.getUser().getUserId() + "@deleted.local";
    }

    private String buildAnonymizedCpf(Student student) {
        long numericCpf = 10_000_000_000L + student.getStudentId();
        String digits = String.format("%011d", numericCpf);
        return digits.substring(0, 3) + "." +
                digits.substring(3, 6) + "." +
                digits.substring(6, 9) + "-" +
                digits.substring(9, 11);
    }
}
