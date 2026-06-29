package com.example.cafe_manager.data.remote;

public class VerifyOtpRequest {
    private int userId;
    private String otpCode;

    public VerifyOtpRequest(int userId, String otpCode) {
        this.userId = userId;
        this.otpCode = otpCode;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }
}
