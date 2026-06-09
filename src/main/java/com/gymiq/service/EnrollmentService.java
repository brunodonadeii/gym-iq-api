package com.gymiq.service;

import java.util.UUID;

import com.gymiq.aop.Auditable;
import com.gymiq.dto.request.EnrollStudentRequest;
import com.gymiq.dto.response.EnrollmentResponse;
import com.gymiq.entity.Enrollment;
import com.gymiq.entity.Enrollment.EnrollmentStatus;
import com.gymiq.entity.Plan;
import com.gymiq.entity.Student;
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
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final int MONTHLY_PLAN_DURATION_MONTHS = 1;
    private static final List<EnrollmentStatus> OPEN_ENROLLMENT_STATUSES = List.of(
            EnrollmentStatus.ACTIVE,
            EnrollmentStatus.SUSPENDED);

    private final EnrollmentRepository enrollmentRepository;
    private final StudentService studentService;
    private final PlanService planService;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> findAll(Pageable pageable) {
        return findActive(pageable);
    }

    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> findActive(Pageable pageable) {
        return enrollmentRepository.findByStatus(EnrollmentStatus.ACTIVE, pageable)
                .map(EnrollmentResponse::fromEntity);
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE_ENROLLMENT, resourceType = ResourceType.ENROLLMENT, description = "Criou matrícula")
    public EnrollmentResponse enroll(EnrollStudentRequest request) {
        Student student = studentService.findEntityById(request.getStudentId());
        Plan plan = planService.findEntityById(request.getPlanId());

        if (!plan.getActive()) {
            throw new BusinessException("O plano selecionado está inativo");
        }
        if (!student.getUser().getActive()) {
            throw new BusinessException("O aluno está inativo e não pode ser matriculado");
        }
        if (enrollmentRepository.existsByStudentStudentIdAndStatusIn(
                student.getStudentId(), OPEN_ENROLLMENT_STATUSES)) {
            throw new BusinessException("Aluno já possui matrícula ativa ou suspensa. Cancele antes de criar outra.");
        }

        LocalDate start = request.getStartDate() != null ? request.getStartDate() : today();
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
    @Auditable(action = AuditAction.UPDATE_ENROLLMENT_STATUS, resourceType = ResourceType.ENROLLMENT, description = "Alterou status da matrícula")
    public EnrollmentResponse changeStatus(UUID enrollmentId, EnrollmentStatus newStatus) {
        Enrollment enrollment = findEntityById(enrollmentId);

        validateStatusTransition(enrollment.getStatus(), newStatus);

        enrollment.setStatus(newStatus);
        enrollment.setCanceledAt(newStatus == EnrollmentStatus.CANCELED ? now() : null);
        enrollmentRepository.save(enrollment);
        log.info("Status da matrícula id={} alterado para {}", enrollmentId, newStatus);

        return buildResponseWithPayments(enrollment);
    }

    @Transactional
    @Auditable(action = AuditAction.RENEW_ENROLLMENT, resourceType = ResourceType.ENROLLMENT, description = "Renovou matrícula")
    public EnrollmentResponse renew(UUID enrollmentId, Integer newPlanId) {
        Enrollment oldEnrollment = findEntityById(enrollmentId);

        if (oldEnrollment.getStatus() == EnrollmentStatus.CANCELED) {
            throw new BusinessException("Não é possível renovar uma matrícula cancelada");
        }

        if (oldEnrollment.getEndDate() == null) {
            throw new BusinessException("Matrícula mensal recorrente não precisa de renovação");
        }

        Plan newPlan = newPlanId != null
                ? planService.findEntityById(newPlanId)
                : oldEnrollment.getPlan();

        if (!newPlan.getActive()) {
            throw new BusinessException("O plano selecionado para renovação está inativo");
        }

        oldEnrollment.setStatus(EnrollmentStatus.CANCELED);
        oldEnrollment.setCanceledAt(now());
        enrollmentRepository.save(oldEnrollment);

        LocalDate start = today();
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

    private LocalDate today() {
        return LocalDate.now(BUSINESS_ZONE);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(BUSINESS_ZONE);
    }

    private void validateStatusTransition(EnrollmentStatus current, EnrollmentStatus next) {
        boolean invalid = switch (current) {
            case CANCELED -> true;
            case ACTIVE -> next == EnrollmentStatus.ACTIVE;
            case SUSPENDED -> next == EnrollmentStatus.SUSPENDED;
        };
        if (invalid) {
            throw new BusinessException(
                    "Transição de status inválida: %s -> %s".formatted(current, next));
        }
    }
}
