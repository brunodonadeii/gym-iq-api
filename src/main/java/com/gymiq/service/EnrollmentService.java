package com.gymiq.service;

import com.gymiq.dto.request.EnrollStudentRequest;
import com.gymiq.dto.response.EnrollmentResponse;
import com.gymiq.entity.Student;
import com.gymiq.entity.Enrollment;
import com.gymiq.entity.Enrollment.EnrollmentStatus;
import com.gymiq.entity.Plan;
import com.gymiq.exception.BusinessException;
import com.gymiq.exception.ResourceNotFoundException;
import com.gymiq.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentService studentService;
    private final PlanService planService;

    @Transactional
    public EnrollmentResponse enroll(EnrollStudentRequest request) {
        Student student = studentService.findEntityById(request.getStudentId());
        Plan plan = planService.findEntityById(request.getPlanId());

        if (!plan.getActive()) {
            throw new BusinessException("O plano selecionado está inativo");
        }
        if (!student.getUser().getActive()) {
            throw new BusinessException("O aluno está inativo e não pode ser matriculado");
        }
        if (enrollmentRepository.existsByStudentStudentIdAndStatus(
                student.getStudentId(), EnrollmentStatus.ACTIVE)) {
            throw new BusinessException("Aluno já possui uma matrícula ativa. Cancele antes de criar outra.");
        }

        LocalDate start = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();
        LocalDate end = start.plusDays(plan.getDurationDays());

        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .plan(plan)
                .startDate(start)
                .endDate(end)
                .status(EnrollmentStatus.ACTIVE)
                .build();

        enrollmentRepository.save(enrollment);
        log.info("Matrícula criada: id={}, aluno={}, plano={}, fim={}",
                enrollment.getEnrollmentId(), student.getStudentId(), plan.getName(), end);

        return EnrollmentResponse.fromEntity(enrollment);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> findByStudent(Integer studentId) {
        studentService.findEntityById(studentId);
        return enrollmentRepository.findByStudentStudentId(studentId)
                .stream()
                .map(EnrollmentResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public EnrollmentResponse findActiveByStudent(Integer studentId) {
        return enrollmentRepository
                .findByStudentStudentIdAndStatus(studentId, EnrollmentStatus.ACTIVE)
                .map(EnrollmentResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nenhuma matrícula ativa encontrada para o aluno: " + studentId));
    }

    @Transactional
    public EnrollmentResponse changeStatus(Integer enrollmentId, EnrollmentStatus newStatus) {
        Enrollment enrollment = findEntityById(enrollmentId);

        validateStatusTransition(enrollment.getStatus(), newStatus);

        enrollment.setStatus(newStatus);
        enrollmentRepository.save(enrollment);
        log.info("Status da matrícula id={} alterado para {}", enrollmentId, newStatus);

        return EnrollmentResponse.fromEntity(enrollment);
    }

    @Transactional
    public EnrollmentResponse renew(Integer enrollmentId, Integer newPlanId) {
        Enrollment oldEnrollment = findEntityById(enrollmentId);

        if (oldEnrollment.getStatus() == EnrollmentStatus.CANCELED) {
            throw new BusinessException("Não é possível renovar uma matrícula cancelada");
        }

        Plan newPlan = newPlanId != null
                ? planService.findEntityById(newPlanId)
                : oldEnrollment.getPlan();

        if (!newPlan.getActive()) {
            throw new BusinessException("O plano selecionado para renovação está inativo");
        }

        oldEnrollment.setStatus(EnrollmentStatus.CANCELED);
        enrollmentRepository.save(oldEnrollment);

        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(newPlan.getDurationDays());

        Enrollment newEnrollment = Enrollment.builder()
                .student(oldEnrollment.getStudent())
                .plan(newPlan)
                .startDate(start)
                .endDate(end)
                .status(EnrollmentStatus.ACTIVE)
                .build();

        enrollmentRepository.save(newEnrollment);
        log.info("Matrícula renovada: nova id={}, aluno={}, plano={}, fim={}",
                newEnrollment.getEnrollmentId(),
                oldEnrollment.getStudent().getStudentId(),
                newPlan.getName(),
                end);

        return EnrollmentResponse.fromEntity(newEnrollment);
    }

    private Enrollment findEntityById(Integer id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Matrícula não encontrada: " + id));
    }

    private void validateStatusTransition(EnrollmentStatus current, EnrollmentStatus next) {
        boolean invalid = switch (current) {
            case CANCELED -> true;
            case ACTIVE   -> next == EnrollmentStatus.ACTIVE;
            case SUSPENDED-> next == EnrollmentStatus.SUSPENDED;
        };
        if (invalid) {
            throw new BusinessException(
                    "Transição de status inválida: %s → %s".formatted(current, next));
        }
    }
}