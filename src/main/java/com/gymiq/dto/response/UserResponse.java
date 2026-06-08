package com.gymiq.dto.response;

import com.gymiq.entity.User;
import com.gymiq.security.PersonalDataExposurePolicy;
import com.gymiq.security.PersonalDataProtection;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserResponse {

    private UUID userId;
    private String name;
    private String email;
    private String role;
    private Boolean active;
    private Boolean lgpdAccepted;
    private LocalDateTime lgpdAcceptedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserResponse fromEntity(User user) {
        boolean fullDataAllowed = PersonalDataExposurePolicy.canViewFullAdministrativeUserData();

        return UserResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(fullDataAllowed ? user.getEmail() : PersonalDataProtection.maskEmail(user.getEmail()))
                .role(user.getRole().name())
                .active(user.getActive())
                .lgpdAccepted(user.getLgpdAccepted())
                .lgpdAcceptedAt(user.getLgpdAcceptedAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
