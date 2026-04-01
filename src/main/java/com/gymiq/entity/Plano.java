package com.gymiq.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Entity
@Table(name = "plano")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plano")
    private Integer idPlano;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "valor_mensal", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorMensal;


    @Column(name = "duracao_dias", nullable = false)
    private Integer duracaoDias;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;


    @OneToMany(mappedBy = "plano", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Matricula> matriculas;
}
