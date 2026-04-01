package com.gymiq.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Table(name = "matricula")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Matricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_matricula")
    private Integer idMatricula;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aluno_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_matricula_aluno"))
    private Aluno aluno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_matricula_plano"))
    private Plano plano;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private StatusMatricula status = StatusMatricula.ATIVO;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;


    public enum StatusMatricula {
        ATIVO,
        SUSPENSO,
        CANCELADO
    }
}
