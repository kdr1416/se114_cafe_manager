package com.example.cafe_manager_api.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_otps")
public class UserOtpEntity {

    @Id
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "otp_code", nullable = false)
    private String otpCode;

    @Column(name = "otp_expiry", nullable = false)
    private Long otpExpiry;
}
