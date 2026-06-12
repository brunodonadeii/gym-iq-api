package com.gymiq.enums;

public enum AuditAction {
    LOGIN("Login"),
    REGISTER("Cadastro"),
    PASSWORD_RESET_REQUESTED("Solicitação de redefinição de senha"),
    PASSWORD_CHANGED("Alteração de senha"),

    CREATE_USER("Criação de usuário"),
    UPDATE_USER("Atualização de usuário"),
    DELETE_USER("Exclusão de usuário"),

    CREATE_STUDENT("Criação de aluno"),
    UPDATE_STUDENT("Atualização de aluno"),
    DEACTIVATE_STUDENT("Inativação de aluno"),
    ACTIVATE_STUDENT("Ativação de aluno"),
    ANONYMIZE_STUDENT("Anonimização de aluno"),

    CREATE_INSTRUCTOR("Criação de instrutor"),
    UPDATE_INSTRUCTOR("Atualização de instrutor"),
    DEACTIVATE_INSTRUCTOR("Inativação de instrutor"),
    ACTIVATE_INSTRUCTOR("Ativação de instrutor"),
    DELETE_INSTRUCTOR("Exclusão de instrutor"),

    CREATE_PLAN("Criação de plano"),
    UPDATE_PLAN("Atualização de plano"),
    DEACTIVATE_PLAN("Inativação de plano"),
    ACTIVATE_PLAN("Ativação de plano"),
    DELETE_PLAN("Exclusão de plano"),

    CREATE_ENROLLMENT("Criação de matrícula"),
    UPDATE_ENROLLMENT_STATUS("Atualização de status da matrícula"),
    RENEW_ENROLLMENT("Renovação de matrícula"),

    PAY_PAYMENT("Pagamento realizado"),
    CHANGE_PAYMENT_STATUS("Alteração de status de pagamento"),
    REFRESH_OVERDUE_PAYMENTS("Atualização de pagamentos vencidos"),
    GENERATE_MONTHLY_PAYMENTS("Geração de pagamentos mensais"),

    CHECK_IN("Check-in"),
    SELF_CHECK_IN("Self check-in"),

    CREATE_EXERCISE("Criação de exercício"),
    UPDATE_EXERCISE("Atualização de exercício"),
    DELETE_EXERCISE("Exclusão de exercício"),

    CREATE_WORKOUT_SHEET("Criação de ficha de treino"),
    UPDATE_WORKOUT_SHEET("Atualização de ficha de treino"),
    DEACTIVATE_WORKOUT_SHEET("Inativação de ficha de treino"),
    ACTIVATE_WORKOUT_SHEET("Ativação de ficha de treino"),
    DELETE_WORKOUT_SHEET("Exclusão de ficha de treino"),

    CREATE_WORKOUT_BLOCK("Criação de treino da ficha"),
    UPDATE_WORKOUT_BLOCK("Atualização de treino da ficha"),
    DELETE_WORKOUT_BLOCK("Remoção de treino da ficha"),

    ADD_WORKOUT_SHEET_EXERCISE("Adição de exercício na ficha"),
    UPDATE_WORKOUT_SHEET_EXERCISE("Atualização de exercício da ficha"),
    DELETE_WORKOUT_SHEET_EXERCISE("Remoção de exercício da ficha"),

    GENERATE_RETENTION_ALERT("Geração de alerta de retenção"),
    GENERATE_RETENTION_ALERTS("Geração de alertas de retenção"),
    RESOLVE_RETENTION_ALERT("Resolução de alerta de retenção");

    private final String label;

    AuditAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
