package com.gymiq.dto.response;

import com.gymiq.entity.Plano;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PlanoResponse {

    private Integer idPlano;
    private String nome;
    private String descricao;
    private BigDecimal valorMensal;
    private Integer duracaoDias;
    private Boolean ativo;
    private LocalDateTime criadoEm;

    public static PlanoResponse fromEntity(Plano plano) {
        return PlanoResponse.builder()
                .idPlano(plano.getIdPlano())
                .nome(plano.getNome())
                .descricao(plano.getDescricao())
                .valorMensal(plano.getValorMensal())
                .duracaoDias(plano.getDuracaoDias())
                .ativo(plano.getAtivo())
                .criadoEm(plano.getCriadoEm())
                .build();
    }
}
