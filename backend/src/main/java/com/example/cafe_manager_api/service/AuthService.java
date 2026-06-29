package com.example.cafe_manager_api.service;

import com.example.cafe_manager_api.dto.LoginRequest;
import com.example.cafe_manager_api.dto.LoginResponse;
import com.example.cafe_manager_api.entity.AuditLogEntity;
import com.example.cafe_manager_api.entity.UserEntity;
import com.example.cafe_manager_api.repository.AuditLogRepository;
import com.example.cafe_manager_api.repository.UserRepository;
import com.example.cafe_manager_api.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private com.example.cafe_manager_api.repository.UserOtpRepository userOtpRepository;

    @Autowired
    private EmailService emailService;

    @Value("${app.jwt.expiration}")
    private long jwtExpirationInMs;

    @Transactional
    public LoginResponse login(LoginRequest loginRequest) {
        String username = loginRequest.getUsername().trim();
        String password = loginRequest.getPassword();

        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Tài khoản không tồn tại."));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new BadCredentialsException("Tài khoản đã bị khóa.");
        }

        boolean authenticated = false;
        boolean migrated = false;

        // 1. Try BCrypt matching
        if (passwordEncoder.matches(password, user.getPasswordHash())) {
            authenticated = true;
        } else {
            // 2. Try legacy SHA-256 matching
            String legacyHash = hashSha256(password);
            if (legacyHash != null && legacyHash.equals(user.getPasswordHash())) {
                authenticated = true;
                migrated = true;
            }
        }

        if (!authenticated) {
            throw new BadCredentialsException("Mật khẩu không đúng.");
        }

        // 3. Migrate password hash if legacy succeeded
        if (migrated) {
            String newBcryptHash = passwordEncoder.encode(password);
            user.setPasswordHash(newBcryptHash);
        }

        // Update last login
        long now = System.currentTimeMillis();
        user.setLastLoginAt(now);
        userRepository.save(user);

        // Log audit trail
        AuditLogEntity log = new AuditLogEntity();
        log.setUserId(user.getUserId());
        log.setAction("LOGIN");
        log.setTargetType("USER");
        log.setTargetId(String.valueOf(user.getUserId()));
        log.setDescription(migrated ? "Đăng nhập thành công (tự động chuyển đổi mật khẩu sang BCrypt)" : "Đăng nhập thành công");
        log.setCreatedAt(now);
        auditLogRepository.save(log);

        if (("ADMIN".equalsIgnoreCase(user.getRole()) || "MANAGER".equalsIgnoreCase(user.getRole()))
                && user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            // Generate 6-digit OTP code
            String otpCode = String.format("%06d", new java.util.Random().nextInt(1000000));
            long expiryTime = System.currentTimeMillis() + 5 * 60 * 1000; // 5 minutes

            // Save user OTP
            com.example.cafe_manager_api.entity.UserOtpEntity userOtp = new com.example.cafe_manager_api.entity.UserOtpEntity(
                    user.getUserId(),
                    otpCode,
                    expiryTime
            );
            userOtpRepository.save(userOtp);

            // Send OTP Email
            emailService.sendOtpEmail(user.getEmail(), otpCode);

            // Return verification required response
            return new LoginResponse(true, "Mã xác thực đăng nhập đã được gửi qua email của bạn.", user.getUserId());
        }

        // Generate token and expiration time
        String token = jwtTokenProvider.generateToken(user);
        long expiresAt = now + jwtExpirationInMs;

        return new LoginResponse(token, user.getUserId(), user.getRole(), user.getFullName(), expiresAt);
    }

    @Transactional
    public LoginResponse verifyOtp(com.example.cafe_manager_api.dto.VerifyOtpRequest request) {
        UserEntity user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BadCredentialsException("Tài khoản không tồn tại."));

        com.example.cafe_manager_api.entity.UserOtpEntity otpRecord = userOtpRepository.findById(request.getUserId())
                .orElseThrow(() -> new BadCredentialsException("Yêu cầu xác thực OTP không tồn tại hoặc đã hết hạn."));

        if (!otpRecord.getOtpCode().equals(request.getOtpCode())) {
            throw new BadCredentialsException("Mã xác thực OTP không chính xác.");
        }

        if (System.currentTimeMillis() > otpRecord.getOtpExpiry()) {
            userOtpRepository.delete(otpRecord);
            throw new BadCredentialsException("Mã xác thực OTP đã hết hạn.");
        }

        // Clear OTP record
        userOtpRepository.delete(otpRecord);

        // Update last login
        long now = System.currentTimeMillis();
        user.setLastLoginAt(now);
        userRepository.save(user);

        // Generate token and expiration time
        String token = jwtTokenProvider.generateToken(user);
        long expiresAt = now + jwtExpirationInMs;

        return new LoginResponse(token, user.getUserId(), user.getRole(), user.getFullName(), expiresAt);
    }

    @Transactional
    public void resendOtp(com.example.cafe_manager_api.dto.ResendOtpRequest request) {
        UserEntity user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BadCredentialsException("Tài khoản không tồn tại."));

        if (!"ADMIN".equalsIgnoreCase(user.getRole()) && !"MANAGER".equalsIgnoreCase(user.getRole())) {
            throw new BadCredentialsException("Vai trò này không yêu cầu xác thực OTP.");
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new BadCredentialsException("Người dùng không đăng ký địa chỉ email để xác thực OTP.");
        }

        // Generate new OTP
        String otpCode = String.format("%06d", new java.util.Random().nextInt(1000000));
        long expiryTime = System.currentTimeMillis() + 5 * 60 * 1000; // 5 minutes

        // Save/Update user OTP
        com.example.cafe_manager_api.entity.UserOtpEntity userOtp = new com.example.cafe_manager_api.entity.UserOtpEntity(
                user.getUserId(),
                otpCode,
                expiryTime
        );
        userOtpRepository.save(userOtp);

        // Send OTP Email
        emailService.sendOtpEmail(user.getEmail(), otpCode);
    }

    private String hashSha256(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
