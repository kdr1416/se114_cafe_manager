package com.example.cafe_manager_api.dto;

import lombok.Data;

@Data
public class VerifyOtpRequest {
    private Integer userId;
    private String otpCode;
}
