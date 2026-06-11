package com.gymiq.service;

import com.gymiq.entity.Enrollment.EnrollmentStatus;
import com.gymiq.entity.User;
import com.gymiq.exception.BusinessException;
import com.gymiq.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentContractService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final String LOGIN_WITHOUT_ACTIVE_ENROLLMENT_MESSAGE =
            "Acesso negado. É necessário possuir uma matrícula ativa para fazer o login.";
    private static final String CHECK_IN_WITHOUT_ACTIVE_ENROLLMENT_MESSAGE =
            "Check-in não permitido. O aluno não possui uma matrícula ativa.";

    private final EnrollmentRepository enrollmentRepository;

    @Transactional(readOnly = true)
    public void validateStudentLoginAccess(User user) {
        if (user.getRole() != User.Role.STUDENT) {
            return;
        }

        if (!hasActiveEnrollmentForUser(user.getUserId())) {
            throw new BusinessException(LOGIN_WITHOUT_ACTIVE_ENROLLMENT_MESSAGE);
        }
    }

    @Transactional(readOnly = true)
    public void validateStudentCheckInAccess(UUID studentId) {
        if (!hasActiveEnrollmentForStudent(studentId)) {
            throw new BusinessException(CHECK_IN_WITHOUT_ACTIVE_ENROLLMENT_MESSAGE);
        }
    }

    private boolean hasActiveEnrollmentForUser(UUID userId) {
        return enrollmentRepository.existsActiveEnrollmentForStudentUser(
                userId,
                EnrollmentStatus.ACTIVE,
                LocalDate.now(BUSINESS_ZONE));
    }

    private boolean hasActiveEnrollmentForStudent(UUID studentId) {
        return enrollmentRepository.existsActiveEnrollmentForStudent(
                studentId,
                EnrollmentStatus.ACTIVE,
                LocalDate.now(BUSINESS_ZONE));
    }
}
