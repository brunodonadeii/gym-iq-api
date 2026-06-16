package com.gymiq.enums;

public enum ResourceType {
    USER("Usuário"),
    STUDENT("Aluno"),
    INSTRUCTOR("Instrutor"),
    PLAN("Plano"),
    ENROLLMENT("Matrícula"),
    PAYMENT("Pagamento"),
    PRESENCE("Presença"),
    EXERCISE("Exercício"),
    WORKOUT_SHEET("Ficha de treino"),
    WORKOUT_BLOCK("Treino da ficha"),
    WORKOUT_SHEET_EXERCISE("Exercício da ficha"),
    RETENTION_ALERT("Alerta de retenção"),
    JOB("Job");

    private final String label;

    ResourceType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
