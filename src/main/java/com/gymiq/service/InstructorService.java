package com.gymiq.service;

import com.gymiq.dto.request.CreateInstructorRequest;
import com.gymiq.dto.response.InstructorResponse;
import com.gymiq.entity.Instructor;
import com.gymiq.entity.User;
import com.gymiq.exception.BusinessException;
import com.gymiq.exception.ResourceNotFoundException;
import com.gymiq.repository.InstructorRepository;
import com.gymiq.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstructorService {

    private final InstructorRepository instructorRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public InstructorResponse create(CreateInstructorRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("E-mail ja cadastrado: " + request.getEmail());
        }
        if (instructorRepository.existsByCref(request.getCref())) {
            throw new BusinessException("CREF ja cadastrado: " + request.getCref());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.INSTRUCTOR)
                .active(true)
                .lgpdAccepted(false)
                .build();
        userRepository.save(user);

        Instructor instructor = Instructor.builder()
                .user(user)
                .cref(request.getCref())
                .phone(request.getPhone())
                .specialty(request.getSpecialty())
                .build();
        instructorRepository.save(instructor);

        log.info("Instructor created: id={}, name={}", instructor.getInstructorId(), user.getName());
        return InstructorResponse.fromEntity(instructor);
    }

    @Transactional(readOnly = true)
    public Page<InstructorResponse> findAll(Pageable pageable) {
        return instructorRepository.findAll(pageable)
                .map(InstructorResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<InstructorResponse> search(String term, Pageable pageable) {
        return instructorRepository.searchByTerm(term, pageable)
                .map(InstructorResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public InstructorResponse findById(Integer id) {
        return InstructorResponse.fromEntity(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public InstructorResponse findByAuthenticatedEmail(String email) {
        return InstructorResponse.fromEntity(findEntityByAuthenticatedEmail(email));
    }

    @Transactional
    public InstructorResponse update(Integer id, CreateInstructorRequest request) {
        Instructor instructor = findEntityById(id);
        User user = instructor.getUser();

        userRepository.findByEmail(request.getEmail())
                .filter(u -> !u.getUserId().equals(user.getUserId()))
                .ifPresent(u -> { throw new BusinessException("E-mail ja usado por outro usuario"); });

        instructorRepository.findByCref(request.getCref())
                .filter(i -> !i.getInstructorId().equals(id))
                .ifPresent(i -> { throw new BusinessException("CREF ja usado por outro instrutor"); });

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        instructor.setCref(request.getCref());
        instructor.setPhone(request.getPhone());
        instructor.setSpecialty(request.getSpecialty());

        instructorRepository.save(instructor);
        log.info("Instructor updated: id={}", id);
        return InstructorResponse.fromEntity(instructor);
    }

    @Transactional
    public void deactivate(Integer id) {
        Instructor instructor = findEntityById(id);
        instructor.getUser().setActive(false);
        instructorRepository.save(instructor);
        log.info("Instructor deactivated: id={}", id);
    }

    public Instructor findEntityById(Integer id) {
        return instructorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Instrutor nao encontrado: " + id));
    }
    public Instructor findEntityByAuthenticatedEmail(String email) {
        return instructorRepository.findByUserEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Instrutor nao encontrado para o usuario autenticado"));
    }
}
