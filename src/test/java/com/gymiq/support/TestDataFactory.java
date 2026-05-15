package com.gymiq.support;

import com.gymiq.entity.Enrollment;
import com.gymiq.entity.Payment;
import com.gymiq.entity.Plan;
import com.gymiq.entity.Presence;
import com.gymiq.entity.RetentionAlert;
import com.gymiq.entity.Student;
import com.gymiq.entity.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static User activeStudentUser() {
        User user = User.builder()
                .name("Ana Silva")
                .email("ana@gymiq.com")
                .passwordHash("encoded-password")
                .role(User.Role.STUDENT)
                .active(true)
                .lgpdAccepted(false)
                .build();
        user.setUserId(10);
        return user;
    }

    public static Student activeStudent() {
        Student student = Student.builder()
                .user(activeStudentUser())
                .cpf("123.456.789-09")
                .birthDate(LocalDate.of(2000, 1, 15))
                .phone("11999999999")
                .zipCode("01001-000")
                .address("Praca da Se")
                .build();
        student.setStudentId(1);
        return student;
    }

    public static Plan activePlan() {
        Plan plan = Plan.builder()
                .name("Mensal")
                .description("Plano mensal")
                .monthlyPrice(BigDecimal.valueOf(99.90))
                .durationDays(30)
                .active(true)
                .build();
        plan.setPlanId(2);
        return plan;
    }

    public static Enrollment activeEnrollment() {
        Enrollment enrollment = Enrollment.builder()
                .student(activeStudent())
                .plan(activePlan())
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().plusDays(20))
                .status(Enrollment.EnrollmentStatus.ACTIVE)
                .build();
        enrollment.setEnrollmentId(3);
        return enrollment;
    }

    public static Payment pendingPayment() {
        Payment payment = Payment.builder()
                .enrollment(activeEnrollment())
                .amount(BigDecimal.valueOf(99.90))
                .dueDate(LocalDate.now().plusDays(5))
                .status(Payment.PaymentStatus.PENDING)
                .build();
        payment.setPaymentId(4);
        return payment;
    }

    public static Presence openPresence() {
        Presence presence = Presence.builder()
                .student(activeStudent())
                .checkInAt(LocalDateTime.now().minusHours(1))
                .notes("Treino livre")
                .build();
        presence.setPresenceId(5);
        return presence;
    }

    public static RetentionAlert openRetentionAlert() {
        RetentionAlert alert = RetentionAlert.builder()
                .student(activeStudent())
                .riskScore(20)
                .riskLevel(RetentionAlert.RiskLevel.LOW)
                .inactiveDays(8)
                .overduePayments(1)
                .message("Risco LOW: 8 dia(s) sem check-in e 1 pagamento(s) atrasado(s).")
                .status(RetentionAlert.AlertStatus.OPEN)
                .build();
        alert.setRetentionAlertId(6);
        return alert;
    }
}
