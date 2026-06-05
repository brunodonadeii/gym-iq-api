package com.gymiq.enums;

public enum AuditAction {
    LOGIN("Login"),
    REGISTER("Cadastro"),
    PASSWORD_RESET_REQUESTED("Solicitacao de redefinicao de senha"),
    PASSWORD_CHANGED("Alteracao de senha"),

    CREATE_USER("Criacao de usuario"),
    UPDATE_USER("Atualizacao de usuario"),
    DELETE_USER("Exclusao de usuario"),

    CREATE_STUDENT("Criacao de aluno"),
    UPDATE_STUDENT("Atualizacao de aluno"),
    DEACTIVATE_STUDENT("Inativacao de aluno"),
    ACTIVATE_STUDENT("Ativacao de aluno"),
    ANONYMIZE_STUDENT("Anonimizacao de aluno"),

    CREATE_INSTRUCTOR("Criacao de instrutor"),
    UPDATE_INSTRUCTOR("Atualizacao de instrutor"),
    DEACTIVATE_INSTRUCTOR("Inativacao de instrutor"),
    ACTIVATE_INSTRUCTOR("Ativacao de instrutor"),
    DELETE_INSTRUCTOR("Exclusao de instrutor"),

    CREATE_PLAN("Criacao de plano"),
    UPDATE_PLAN("Atualizacao de plano"),
    DEACTIVATE_PLAN("Inativacao de plano"),
    ACTIVATE_PLAN("Ativacao de plano"),
    DELETE_PLAN("Exclusao de plano"),

    CREATE_ENROLLMENT("Criacao de matricula"),
    UPDATE_ENROLLMENT_STATUS("Atualizacao de status da matricula"),
    RENEW_ENROLLMENT("Renovacao de matricula"),

    PAY_PAYMENT("Pagamento realizado"),
    CHANGE_PAYMENT_STATUS("Alteracao de status de pagamento"),
    REFRESH_OVERDUE_PAYMENTS("Atualizacao de pagamentos vencidos"),
    GENERATE_MONTHLY_PAYMENTS("Geracao de pagamentos mensais"),

    CHECK_IN("Check-in"),
    SELF_CHECK_IN("Self check-in"),

    CREATE_EXERCISE("Criacao de exercicio"),
    UPDATE_EXERCISE("Atualizacao de exercicio"),
    DELETE_EXERCISE("Exclusao de exercicio"),

    CREATE_WORKOUT_SHEET("Criacao de ficha de treino"),
    UPDATE_WORKOUT_SHEET("Atualizacao de ficha de treino"),
    DEACTIVATE_WORKOUT_SHEET("Inativacao de ficha de treino"),

    ADD_WORKOUT_SHEET_EXERCISE("Adicao de exercicio na ficha"),
    UPDATE_WORKOUT_SHEET_EXERCISE("Atualizacao de exercicio da ficha"),
    DELETE_WORKOUT_SHEET_EXERCISE("Remocao de exercicio da ficha"),

    GENERATE_RETENTION_ALERT("Geracao de alerta de retencao"),
    GENERATE_RETENTION_ALERTS("Geracao de alertas de retencao"),
    RESOLVE_RETENTION_ALERT("Resolucao de alerta de retencao");

    private final String label;

    AuditAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
