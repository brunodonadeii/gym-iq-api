package com.gymiq.service;

import com.gymiq.dto.request.CreateStudentRequest;
import com.gymiq.dto.response.StudentResponse;
import com.gymiq.entity.Student;
import com.gymiq.entity.User;
import com.gymiq.exception.BusinessException;
import com.gymiq.exception.ResourceNotFoundException;
import com.gymiq.repository.StudentRepository;
import com.gymiq.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public StudentResponse create(CreateStudentRequest request) {
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
                .lgpdAccepted(false)
                .build();
        userRepository.save(user);

        Student student = Student.builder()
                .user(user)
                .cpf(request.getCpf())
                .birthDate(request.getBirthDate())
                .phone(request.getPhone())
                .zipCode(request.getZipCode())
                .address(request.getAddress())
                .build();
        studentRepository.save(student);

        log.info("Student created: id={}, name={}", student.getStudentId(), user.getName());
        return StudentResponse.fromEntity(student);
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> findAll() {
        return studentRepository.findAll()
                .stream()
                .map(StudentResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> search(String term) {
        return studentRepository.searchByTerm(term)
                .stream()
                .map(StudentResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentResponse findById(Integer id) {
        return StudentResponse.fromEntity(findEntityById(id));
    }

    @Transactional
    public StudentResponse update(Integer id, CreateStudentRequest request) {
        Student student = findEntityById(id);
        User user = student.getUser();

        userRepository.findByEmail(request.getEmail())
                .filter(u -> !u.getUserId().equals(user.getUserId()))
                .ifPresent(u -> { throw new BusinessException("E-mail já usado por outro usuário"); });

        studentRepository.findByCpf(request.getCpf())
                .filter(s -> !s.getStudentId().equals(id))
                .ifPresent(s -> { throw new BusinessException("CPF já usado por outro aluno"); });

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        student.setCpf(request.getCpf());
        student.setBirthDate(request.getBirthDate());
        student.setPhone(request.getPhone());
        student.setZipCode(request.getZipCode());
        student.setAddress(request.getAddress());

        studentRepository.save(student);
        log.info("Student updated: id={}", id);
        return StudentResponse.fromEntity(student);
    }

    @Transactional
    public void deactivate(Integer id) {
        Student student = findEntityById(id);
        student.getUser().setActive(false);
        studentRepository.save(student);
        log.info("Student deactivated: id={}", id);
    }

    public Student findEntityById(Integer id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado: " + id));
    }
}