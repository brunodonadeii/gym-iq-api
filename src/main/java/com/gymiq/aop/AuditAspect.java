package com.gymiq.aop;

import com.gymiq.enums.ResourceType;
import com.gymiq.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private static final Map<ResourceType, String> RESOURCE_ID_GETTERS = Map.of(
            ResourceType.USER, "getUserId",
            ResourceType.STUDENT, "getStudentId",
            ResourceType.INSTRUCTOR, "getInstructorId",
            ResourceType.PLAN, "getPlanId",
            ResourceType.ENROLLMENT, "getEnrollmentId",
            ResourceType.PAYMENT, "getPaymentId",
            ResourceType.PRESENCE, "getPresenceId",
            ResourceType.EXERCISE, "getExerciseId",
            ResourceType.WORKOUT_SHEET, "getWorkoutSheetId",
            ResourceType.WORKOUT_SHEET_EXERCISE, "getWorkoutSheetExerciseId"
    );

    private final AuditLogService auditLogService;

    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void afterSuccessfulAuditableMethod(JoinPoint joinPoint, Auditable auditable, Object result) {
        try {
            Integer resourceId = resolveResourceId(joinPoint.getArgs(), result, auditable.resourceType());
            auditLogService.record(
                    auditable.action(),
                    auditable.resourceType(),
                    resourceId,
                    auditable.description());
        } catch (Exception exception) {
            log.error("AuditAspect falhou sem interromper a operacao principal: {}", exception.getMessage());
        }
    }

    private Integer resolveResourceId(Object[] args, Object result, ResourceType resourceType) {
        Integer idFromResult = resolveResourceIdFromResult(result, resourceType);
        if (idFromResult != null) {
            return idFromResult;
        }

        if (args == null) {
            return null;
        }

        for (Object arg : args) {
            if (arg instanceof Integer id) {
                return id;
            }
        }

        return null;
    }

    private Integer resolveResourceIdFromResult(Object result, ResourceType resourceType) {
        if (result == null) {
            return null;
        }

        String getterName = RESOURCE_ID_GETTERS.get(resourceType);
        if (getterName == null) {
            return null;
        }

        try {
            Method getter = result.getClass().getMethod(getterName);
            Object value = getter.invoke(result);
            return value instanceof Integer id ? id : null;
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }
}
