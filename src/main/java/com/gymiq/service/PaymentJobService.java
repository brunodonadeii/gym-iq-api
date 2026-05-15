package com.gymiq.service;

import com.gymiq.entity.Enrollment;
import com.gymiq.entity.Enrollment.EnrollmentStatus;
import com.gymiq.entity.Payment;
import com.gymiq.entity.Payment.PaymentStatus;
import com.gymiq.repository.EnrollmentRepository;
import com.gymiq.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentJobService {

    private final PaymentRepository paymentRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public int refreshOverduePayments() {
        List<Payment> overduePayments = paymentRepository
                .findByStatusAndDueDateBefore(PaymentStatus.PENDING, LocalDate.now());

        overduePayments.forEach(payment -> payment.setStatus(PaymentStatus.OVERDUE));
        paymentRepository.saveAll(overduePayments);

        log.info("Job de pagamentos vencidos executado. Atualizados={}", overduePayments.size());
        return overduePayments.size();
    }

    @Transactional
    public int generateMonthlyPayments() {
        LocalDate today = LocalDate.now();
        List<Enrollment> activeEnrollments = enrollmentRepository.findByStatus(EnrollmentStatus.ACTIVE);
        int createdPayments = 0;

        for (Enrollment enrollment : activeEnrollments) {
            LocalDate nextDueDate = paymentRepository
                    .findTopByEnrollmentEnrollmentIdOrderByDueDateDesc(enrollment.getEnrollmentId())
                    .map(payment -> payment.getDueDate().plusMonths(1))
                    .orElse(enrollment.getStartDate());

            while (!nextDueDate.isAfter(today) && !nextDueDate.isAfter(enrollment.getEndDate())) {
                if (!paymentRepository.existsByEnrollmentEnrollmentIdAndDueDate(
                        enrollment.getEnrollmentId(), nextDueDate)) {
                    createMonthlyPayment(enrollment, nextDueDate);
                    createdPayments++;
                }

                nextDueDate = nextDueDate.plusMonths(1);
            }
        }

        log.info("Job de mensalidades executado. Criados={}", createdPayments);
        return createdPayments;
    }

    private void createMonthlyPayment(Enrollment enrollment, LocalDate dueDate) {
        Payment payment = Payment.builder()
                .enrollment(enrollment)
                .amount(enrollment.getPlan().getMonthlyPrice())
                .dueDate(dueDate)
                .status(resolveInitialStatus(dueDate))
                .notes("Mensalidade gerada automaticamente")
                .build();

        paymentRepository.save(payment);
    }

    private PaymentStatus resolveInitialStatus(LocalDate dueDate) {
        return dueDate.isBefore(LocalDate.now()) ? PaymentStatus.OVERDUE : PaymentStatus.PENDING;
    }
}
