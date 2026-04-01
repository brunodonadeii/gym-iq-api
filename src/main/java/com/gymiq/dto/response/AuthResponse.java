package com.gymiq.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;
    private String tipo;
    private Integer idUsuario;
    private String nome;
    private String email;
    private String perfil;
    private Boolean lgpdAceito;
}
