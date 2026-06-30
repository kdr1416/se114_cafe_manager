package com.example.cafe_manager_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private Integer userId;
    private String role;
    private String fullName;
    private Long expiresAt;
    private Boolean requiresVerification;
    private String message;

    public LoginResponse(String token, Integer userId, String role, String fullName, Long expiresAt) {
        this.token = token;
        this.userId = userId;
        this.role = role;
        this.fullName = fullName;
        this.expiresAt = expiresAt;
        this.requiresVerification = false;
        this.message = null;
    }

    public LoginResponse(Boolean requiresVerification, String message, Integer userId) {
        this.token = null;
        this.userId = userId;
        this.role = null;
        this.fullName = null;
        this.expiresAt = 0L;
        this.requiresVerification = requiresVerification;
        this.message = message;
    }
}
