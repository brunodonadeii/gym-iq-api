package com.gymiq.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AuthResponse {
    private String token;
    private String type;
    private UUID userId;
    private String name;
    private String email;
    private String role;
    private Boolean lgpdAccepted;
}
