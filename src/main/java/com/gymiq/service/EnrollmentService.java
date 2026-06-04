package com.gymiq.service;

import java.util.UUID;

import com.gymiq.aop.Auditable;
import com.gymiq.dto.request.EnrollStudentRequest;
import com.gymiq.dto.response.EnrollmentResponse;
import com.gymiq.entity.Student;
import com.gymiq.entity.Enrollment;
import com.gymiq.entity.Enrollment.EnrollmentStatus;
import com.gymiq.entity.Plan;
import com.gymiq.enums.AuditAction;
import com.gymiq.enums.ResourceType;
import com.gymiq.exception.BusinessException;
import com.gymiq.exception.ResourceNotFoundException;
import com.gymiq.repository.EnrollmentRepository;
import com.gymiq.repository.PaymentRepository;
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
public class EnrollmentService {

    private static final int MONTHLY_PLAN_DURATION_MONTHS = 1;

    private final EnrollmentRepository enrollmentRepository;
    private final StudentService studentService;
    private final PlanService planService;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;


    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> findAll(Pageable pageable) {
        return enrollmentRepository.findAll(pageable)
                .map(EnrollmentResponse::fromEntity);
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE_ENROLLMENT, resourceType = ResourceType.ENROLLMENT, description = "Criou matricula")
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
        LocalDate end = calculateEndDate(start, plan);

        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .plan(plan)
                .startDate(start)
                .endDate(end)
                .status(EnrollmentStatus.ACTIVE)
                .build();

        enrollmentRepository.save(enrollment);

        paymentService.createFirstPaymentForEnrollment(enrollment);
        log.info("Matrícula criada: id={}, aluno={}, plano={}, fim={}",
                enrollment.getEnrollmentId(), student.getStudentId(), plan.getName(), end);

        return buildResponseWithPayments(enrollment);
    }

    @Transactional(readOnly = true)
    public EnrollmentResponse findById(UUID enrollmentId) {
        return buildResponseWithPayments(findEntityById(enrollmentId));
    }

    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> findByStudent(UUID studentId, Pageable pageable) {
        studentService.findEntityById(studentId);
        return enrollmentRepository.findByStudentStudentId(studentId, pageable)
                .map(EnrollmentResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> findByAuthenticatedStudent(String email, Pageable pageable) {
        Student student = studentService.findEntityByAuthenticatedEmail(email);
        return enrollmentRepository.findByStudentStudentId(student.getStudentId(), pageable)
                .map(EnrollmentResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public EnrollmentResponse findActiveByStudent(UUID studentId) {
        return enrollmentRepository
                .findByStudentStudentIdAndStatus(studentId, EnrollmentStatus.ACTIVE)
                .map(this::buildResponseWithPayments)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nenhuma matrícula ativa encontrada para o aluno: " + studentId));
    }

    @Transactional(readOnly = true)
    public EnrollmentResponse findActiveByAuthenticatedStudent(String email) {
        Student student = studentService.findEntityByAuthenticatedEmail(email);
        return findActiveByStudent(student.getStudentId());
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE_ENROLLMENT_STATUS, resourceType = ResourceType.ENROLLMENT, description = "Alterou status da matricula")
    public EnrollmentResponse changeStatus(UUID enrollmentId, EnrollmentStatus newStatus) {
        Enrollment enrollment = findEntityById(enrollmentId);

        validateStatusTransition(enrollment.getStatus(), newStatus);

        enrollment.setStatus(newStatus);
        enrollment.setCanceledAt(newStatus == EnrollmentStatus.CANCELED ? LocalDateTime.now() : null);
        enrollmentRepository.save(enrollment);
        log.info("Status da matrícula id={} alterado para {}", enrollmentId, newStatus);

        return buildResponseWithPayments(enrollment);
    }

    @Transactional
    @Auditable(action = AuditAction.RENEW_ENROLLMENT, resourceType = ResourceType.ENROLLMENT, description = "Renovou matricula")
    public EnrollmentResponse renew(UUID enrollmentId, Integer newPlanId) {
        Enrollment oldEnrollment = findEntityById(enrollmentId);

        if (oldEnrollment.getStatus() == EnrollmentStatus.CANCELED) {
            throw new BusinessException("Não é possível renovar uma matrícula cancelada");
        }

        if (oldEnrollment.getEndDate() == null) {
            throw new BusinessException("Matricula mensal recorrente nao precisa de renovacao");
        }

        Plan newPlan = newPlanId != null
                ? planService.findEntityById(newPlanId)
                : oldEnrollment.getPlan();

        if (!newPlan.getActive()) {
            throw new BusinessException("O plano selecionado para renovação está inativo");
        }

        oldEnrollment.setStatus(EnrollmentStatus.CANCELED);
        oldEnrollment.setCanceledAt(LocalDateTime.now());
        enrollmentRepository.save(oldEnrollment);

        LocalDate start = LocalDate.now();
        LocalDate end = calculateEndDate(start, newPlan);

        Enrollment newEnrollment = Enrollment.builder()
                .student(oldEnrollment.getStudent())
                .plan(newPlan)
                .startDate(start)
                .endDate(end)
                .status(EnrollmentStatus.ACTIVE)
                .build();

        enrollmentRepository.save(newEnrollment);
        paymentService.createFirstPaymentForEnrollment(newEnrollment);
        log.info("Matrícula renovada: nova id={}, aluno={}, plano={}, fim={}",
                newEnrollment.getEnrollmentId(),
                oldEnrollment.getStudent().getStudentId(),
                newPlan.getName(),
                end);

        return buildResponseWithPayments(newEnrollment);
    }

    private Enrollment findEntityById(UUID id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Matrícula não encontrada: " + id));
    }

    private EnrollmentResponse buildResponseWithPayments(Enrollment enrollment) {
        return EnrollmentResponse.fromEntity(
                enrollment,
                paymentRepository.findByEnrollmentEnrollmentIdOrderByDueDateDesc(enrollment.getEnrollmentId()));
    }

    private LocalDate calculateEndDate(LocalDate startDate, Plan plan) {
        if (isMonthlyPlan(plan)) {
            return null;
        }
        return startDate.plusMonths(plan.getDurationMonths());
    }

    private boolean isMonthlyPlan(Plan plan) {
        return MONTHLY_PLAN_DURATION_MONTHS == plan.getDurationMonths();
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
