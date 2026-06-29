package com.example.cafe_manager_api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String otpCode) {
        System.out.println("==================================================");
        System.out.println("SENDING OTP EMAIL TO: " + toEmail);
        System.out.println("OTP CODE: " + otpCode);
        System.out.println("==================================================");

        if (toEmail == null || toEmail.trim().isEmpty()) {
            System.out.println("No email registered for this user. OTP code printed to console only.");
            return;
        }

        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(toEmail);
                message.setSubject("Mã xác thực đăng nhập - Cafe Manager");
                message.setText("Mã OTP của bạn là: " + otpCode + "\nMã có hiệu lực trong vòng 5 phút.");
                mailSender.send(message);
                System.out.println("OTP email sent successfully via SMTP.");
            } catch (Exception e) {
                System.err.println("Failed to send email via SMTP: " + e.getMessage());
            }
        } else {
            System.out.println("JavaMailSender is not configured. Email printing to console only.");
        }
    }
}
