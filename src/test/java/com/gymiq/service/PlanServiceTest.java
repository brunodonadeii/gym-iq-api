package com.gymiq.service;

import com.gymiq.dto.request.CreatePlanRequest;
import com.gymiq.dto.response.PlanResponse;
import com.gymiq.entity.Plan;
import com.gymiq.exception.BusinessException;
import com.gymiq.repository.PlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock
    private PlanRepository planRepository;

    @InjectMocks
    private PlanService planService;

    @Test
    void createShouldPersistActivePlan() {
        CreatePlanRequest request = validPlanRequest();

        when(planRepository.existsByNameIgnoreCase(request.getName())).thenReturn(false);
        when(planRepository.save(any(Plan.class))).thenAnswer(invocation -> {
            Plan plan = invocation.getArgument(0);
            plan.setPlanId(2);
            return plan;
        });

        PlanResponse response = planService.create(request);

        assertThat(response.getPlanId()).isEqualTo(2);
        assertThat(response.getName()).isEqualTo("Mensal");
        assertThat(response.getActive()).isTrue();
    }

    @Test
    void createShouldRejectDuplicatedPlanName() {
        CreatePlanRequest request = validPlanRequest();

        when(planRepository.existsByNameIgnoreCase(request.getName())).thenReturn(true);

        assertThatThrownBy(() -> planService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("plano");

        verify(planRepository, never()).save(any(Plan.class));
    }

    @Test
    void deactivateShouldMarkPlanAsInactive() {
        Plan plan = Plan.builder()
                .name("Mensal")
                .monthlyPrice(BigDecimal.valueOf(99.90))
                .durationDays(30)
                .active(true)
                .build();
        plan.setPlanId(2);

        when(planRepository.findById(plan.getPlanId())).thenReturn(Optional.of(plan));

        planService.deactivate(plan.getPlanId());

        assertThat(plan.getActive()).isFalse();
        verify(planRepository).save(plan);
    }

    private CreatePlanRequest validPlanRequest() {
        CreatePlanRequest request = new CreatePlanRequest();
        request.setName("Mensal");
        request.setDescription("Plano mensal");
        request.setMonthlyPrice(BigDecimal.valueOf(99.90));
        request.setDurationDays(30);
        return request;
    }
}
