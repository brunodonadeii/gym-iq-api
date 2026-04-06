package com.gymiq.service;

import com.gymiq.dto.request.CreatePlanRequest;
import com.gymiq.dto.response.PlanResponse;
import com.gymiq.entity.Plan;
import com.gymiq.exception.BusinessException;
import com.gymiq.exception.ResourceNotFoundException;
import com.gymiq.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;

    @Transactional
    public PlanResponse create(CreatePlanRequest request) {
        if (planRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BusinessException("Já existe um plano com o nome: " + request.getName());
        }

        Plan plan = Plan.builder()
                .name(request.getName())
                .description(request.getDescription())
                .monthlyPrice(request.getMonthlyPrice())
                .durationDays(request.getDurationDays())
                .active(true)
                .build();

        planRepository.save(plan);
        log.info("Plan created: id={}, name={}", plan.getPlanId(), plan.getName());
        return PlanResponse.fromEntity(plan);
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> findActive() {
        return planRepository.findByActiveTrue()
                .stream()
                .map(PlanResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> findAll() {
        return planRepository.findAll()
                .stream()
                .map(PlanResponse::fromEntity)
                .toList();
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
        plan.setDurationDays(request.getDurationDays());

        planRepository.save(plan);
        log.info("Plan updated: id={}", id);
        return PlanResponse.fromEntity(plan);
    }

    @Transactional
    public void deactivate(Integer id) {
        Plan plan = findEntityById(id);
        plan.setActive(false);
        planRepository.save(plan);
        log.info("Plan deactivated: id={}", id);
    }

    public Plan findEntityById(Integer id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado: " + id));
    }
}