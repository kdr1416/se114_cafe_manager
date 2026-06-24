package com.example.cafe_manager.data.remote;

public class UpdateUserRequest {
    private String fullName;
    private String phone;
    private String role;

    public UpdateUserRequest() {}

    public UpdateUserRequest(String fullName, String phone, String role) {
        this.fullName = fullName;
        this.phone = phone;
        this.role = role;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
