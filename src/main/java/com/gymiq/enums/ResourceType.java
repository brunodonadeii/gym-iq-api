package com.gymiq.enums;

public enum ResourceType {
    USER("Usuario"),
    STUDENT("Aluno"),
    INSTRUCTOR("Instrutor"),
    PLAN("Plano"),
    ENROLLMENT("Matricula"),
    PAYMENT("Pagamento"),
    PRESENCE("Presenca"),
    EXERCISE("Exercicio"),
    WORKOUT_SHEET("Ficha de treino"),
    WORKOUT_BLOCK("Treino da ficha"),
    WORKOUT_SHEET_EXERCISE("Exercicio da ficha"),
    RETENTION_ALERT("Alerta de retencao"),
    JOB("Job");

    private final String label;

    ResourceType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
