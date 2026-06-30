package com.example.cafe_manager.data.remote;

public class AssignStaffRequest {
    private Integer userId;
    private String role;

    public AssignStaffRequest() {}

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
