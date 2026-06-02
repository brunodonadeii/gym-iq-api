package com.gymiq.dto.response;

import com.gymiq.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {

    private Integer userId;
    private String name;
    private String email;
    private String role;
    private Boolean active;
    private Boolean lgpdAccepted;
    private LocalDateTime lgpdAcceptedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .active(user.getActive())
                .lgpdAccepted(user.getLgpdAccepted())
                .lgpdAcceptedAt(user.getLgpdAcceptedAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
