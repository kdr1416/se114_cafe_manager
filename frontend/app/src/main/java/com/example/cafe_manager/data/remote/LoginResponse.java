package com.example.cafe_manager.data.remote;

public class LoginResponse {
    private String token;
    private int userId;
    private String role;
    private String fullName;
    private long expiresAt;
    private Boolean requiresVerification;
    private String message;

    public LoginResponse() {}

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }

    public Boolean getRequiresVerification() { return requiresVerification; }
    public void setRequiresVerification(Boolean requiresVerification) { this.requiresVerification = requiresVerification; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
