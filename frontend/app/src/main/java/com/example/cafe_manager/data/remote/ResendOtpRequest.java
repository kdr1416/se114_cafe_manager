package com.example.cafe_manager.data.remote;

public class ResendOtpRequest {
    private int userId;

    public ResendOtpRequest(int userId) {
        this.userId = userId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}
