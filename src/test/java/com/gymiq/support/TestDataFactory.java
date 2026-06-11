package com.gymiq.support;

import com.gymiq.entity.Enrollment;
import com.gymiq.entity.Exercise;
import com.gymiq.entity.Instructor;
import com.gymiq.entity.Payment;
import com.gymiq.entity.Plan;
import com.gymiq.entity.Presence;
import com.gymiq.entity.RetentionAlert;
import com.gymiq.entity.Student;
import com.gymiq.entity.User;
import com.gymiq.entity.WorkoutBlock;
import com.gymiq.entity.WorkoutSheet;
import com.gymiq.entity.WorkoutSheetExercise;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

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
        user.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        return user;
    }

    public static User activeInstructorUser() {
        User user = User.builder()
                .name("Carlos Trainer")
                .email("carlos@gymiq.com")
                .passwordHash("encoded-password")
                .role(User.Role.INSTRUCTOR)
                .active(true)
                .lgpdAccepted(true)
                .build();
        user.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000020"));
        return user;
    }

    public static User activeAdminUser() {
        User user = User.builder()
                .name("Admin GymIQ")
                .email("admin@gymiq.com")
                .passwordHash("encoded-password")
                .role(User.Role.ADMIN)
                .active(true)
                .lgpdAccepted(true)
                .build();
        user.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000030"));
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
        student.setStudentId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        return student;
    }

    public static Plan activePlan() {
        Plan plan = Plan.builder()
                .name("Mensal")
                .description("Plano mensal")
                .monthlyPrice(BigDecimal.valueOf(99.90))
                .durationMonths(1)
                .active(true)
                .build();
        plan.setPlanId(2);
        return plan;
    }

    public static Instructor activeInstructor() {
        Instructor instructor = Instructor.builder()
                .user(activeInstructorUser())
                .cref("123456-G/SP")
                .phone("11988887777")
                .specialty("Musculacao")
                .build();
        instructor.setInstructorId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        return instructor;
    }

    public static Exercise exercise() {
        Exercise exercise = Exercise.builder()
                .name("Supino")
                .muscleGroup("Peito")
                .description("Exercicio de forca")
                .build();
        exercise.setExerciseId(7);
        return exercise;
    }

    public static Enrollment activeEnrollment() {
        Enrollment enrollment = Enrollment.builder()
                .student(activeStudent())
                .plan(activePlan())
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().plusDays(20))
                .status(Enrollment.EnrollmentStatus.ACTIVE)
                .build();
        enrollment.setEnrollmentId(UUID.fromString("00000000-0000-0000-0000-000000000003"));
        return enrollment;
    }

    public static Payment pendingPayment() {
        Payment payment = Payment.builder()
                .enrollment(activeEnrollment())
                .amount(BigDecimal.valueOf(99.90))
                .dueDate(LocalDate.now().plusDays(5))
                .status(Payment.PaymentStatus.PENDING)
                .build();
        payment.setPaymentId(UUID.fromString("00000000-0000-0000-0000-000000000004"));
        return payment;
    }

    public static Presence openPresence() {
        Presence presence = Presence.builder()
                .student(activeStudent())
                .checkInAt(LocalDateTime.now().minusHours(1))
                .notes("Treino livre")
                .build();
        presence.setPresenceId(UUID.fromString("00000000-0000-0000-0000-000000000005"));
        return presence;
    }

    public static WorkoutSheet workoutSheet() {
        WorkoutSheet workoutSheet = WorkoutSheet.builder()
                .student(activeStudent())
                .instructor(activeInstructor())
                .name("Ficha Hipertrofia")
                .goal("Ganho de massa")
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 8, 1))
                .active(true)
                .notes("Ajustar cargas semanalmente")
                .blocks(new ArrayList<>())
                .build();
        workoutSheet.setWorkoutSheetId(UUID.fromString("00000000-0000-0000-0000-000000000008"));

        WorkoutBlock block = workoutBlock(workoutSheet);
        workoutSheet.getBlocks().add(block);
        return workoutSheet;
    }

    public static WorkoutBlock workoutBlock(WorkoutSheet workoutSheet) {
        WorkoutBlock block = WorkoutBlock.builder()
                .workoutSheet(workoutSheet)
                .name("Treino A")
                .description("Peito, ombro e triceps")
                .executionOrder(1)
                .active(true)
                .exercises(new ArrayList<>())
                .build();
        block.setWorkoutBlockId(UUID.fromString("00000000-0000-0000-0000-000000000088"));

        WorkoutSheetExercise exerciseItem = workoutSheetExercise(block);
        block.getExercises().add(exerciseItem);
        return block;
    }

    public static WorkoutSheetExercise workoutSheetExercise(WorkoutBlock workoutBlock) {
        WorkoutSheetExercise item = WorkoutSheetExercise.builder()
                .workoutBlock(workoutBlock)
                .exercise(exercise())
                .sets(4)
                .repetitions("10")
                .restSeconds(60)
                .executionOrder(1)
                .notes("Controlar movimento")
                .build();
        item.setWorkoutSheetExerciseId(UUID.fromString("00000000-0000-0000-0000-000000000009"));
        return item;
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
        alert.setRetentionAlertId(UUID.fromString("00000000-0000-0000-0000-000000000006"));
        return alert;
    }
}
