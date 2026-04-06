package com.gymiq.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;
    private String type;
    private Integer userId;
    private String name;
    private String email;
    private String role;
    private Boolean lgpdAccepted;
}