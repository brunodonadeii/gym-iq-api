package com.gymiq.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "aluno",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_aluno_cpf", columnNames = "cpf")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Aluno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_aluno")
    private Integer idAluno;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_aluno_usuario"))
    private Usuario usuario;

    @Column(name = "cpf", nullable = false, length = 14)
    private String cpf;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Column(name = "telefone", nullable = false, length = 20)
    private String telefone;

    @Column(name = "cep", length = 9)
    private String cep;

    @Column(name = "endereco", length = 255)
    private String endereco;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;



    @OneToMany(mappedBy = "aluno", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Matricula> matriculas;
}
