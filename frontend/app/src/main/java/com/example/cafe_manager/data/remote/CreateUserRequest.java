package com.example.cafe_manager.data.remote;

public class CreateUserRequest {
    private String username;
    private String password;
    private String fullName;
    private String phone;
    private String role;

    public CreateUserRequest() {}

    public CreateUserRequest(String username, String password, String fullName, String phone, String role) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.phone = phone;
        this.role = role;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
