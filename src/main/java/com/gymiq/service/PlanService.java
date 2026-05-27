package com.gymiq.service;

import com.gymiq.dto.request.CreatePlanRequest;
import com.gymiq.dto.response.PlanResponse;
import com.gymiq.entity.Plan;
import com.gymiq.exception.BusinessException;
import com.gymiq.exception.ResourceNotFoundException;
import com.gymiq.repository.EnrollmentRepository;
import com.gymiq.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public PlanResponse create(CreatePlanRequest request) {
        if (planRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BusinessException("Já existe um plano com o nome: " + request.getName());
        }

        Plan plan = Plan.builder()
                .name(request.getName())
                .description(request.getDescription())
                .monthlyPrice(request.getMonthlyPrice())
                .durationMonths(request.getDurationMonths())
                .active(true)
                .build();

        planRepository.save(plan);
        log.info("Plan created: id={}, name={}", plan.getPlanId(), plan.getName());
        return PlanResponse.fromEntity(plan);
    }

    @Transactional(readOnly = true)
    public Page<PlanResponse> findActive(Pageable pageable) {
        return planRepository.findByActiveTrue(pageable)
                .map(PlanResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<PlanResponse> findAll(Pageable pageable) {
        return planRepository.findAll(pageable)
                .map(PlanResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public PlanResponse findById(Integer id) {
        return PlanResponse.fromEntity(findEntityById(id));
    }

    @Transactional
    public PlanResponse update(Integer id, CreatePlanRequest request) {
        Plan plan = findEntityById(id);

        planRepository.findAll().stream()
                .filter(p -> p.getName().equalsIgnoreCase(request.getName())
                        && !p.getPlanId().equals(id))
                .findFirst()
                .ifPresent(p -> { throw new BusinessException("Nome já usado por outro plano"); });

        plan.setName(request.getName());
        plan.setDescription(request.getDescription());
        plan.setMonthlyPrice(request.getMonthlyPrice());
        plan.setDurationMonths(request.getDurationMonths());

        planRepository.save(plan);
        log.info("Plan updated: id={}", id);
        return PlanResponse.fromEntity(plan);
    }

    @Transactional
    public void delete(Integer id) {
        Plan plan = findEntityById(id);

        if (enrollmentRepository.existsByPlanPlanId(id)) {
            throw new BusinessException("NÃ£o Ã© possÃ­vel excluir um plano vinculado a matrÃ­culas");
        }

        planRepository.delete(plan);
        log.info("Plan deleted: id={}", id);
    }

    @Transactional
    public void deactivate(Integer id) {
        Plan plan = findEntityById(id);
        plan.setActive(false);
        planRepository.save(plan);
        log.info("Plan deactivated: id={}", id);
    }

    @Transactional
    public void activate(Integer id) {
        Plan plan = findEntityById(id);
        plan.setActive(true);
        planRepository.save(plan);
        log.info("Plan activated: id={}", id);
    }

    public Plan findEntityById(Integer id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado: " + id));
    }
}
