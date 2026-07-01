package com.example.cafe_manager.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.cafe_manager.R;
import com.example.cafe_manager.manager.SessionManager;
import com.example.cafe_manager.ui.table.TableActivity;
import com.example.cafe_manager.viewmodel.AuthViewModel;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private AuthViewModel viewModel;

    private TextInputLayout tilUsername;
    private TextInputLayout tilPassword;
    private EditText etUsername;
    private EditText etPassword;
    private Button btnLogin;

    // OTP Views & States
    private View layoutCredentials;
    private View layoutOtp;
    private TextInputLayout tilOtp;
    private EditText etOtp;
    private TextView tvOtpDescription;
    private TextView btnResendOtp;
    private Button btnConfirmOtp;
    private Button btnBackToLogin;

    private int otpUserId = 0;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Splash check — đã login rồi thì skip thẳng vào Tables
        if (SessionManager.getInstance(this).isLoggedIn()) {
            navigateToTables();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        bindViews();
        setupViewModel();
        setupInputListeners();
    }

    private void bindViews() {
        tilUsername = findViewById(R.id.til_username);
        tilPassword = findViewById(R.id.til_password);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);

        layoutCredentials = findViewById(R.id.layout_credentials);
        layoutOtp = findViewById(R.id.layout_otp);
        tilOtp = findViewById(R.id.til_otp);
        etOtp = findViewById(R.id.et_otp);
        tvOtpDescription = findViewById(R.id.tv_otp_description);
        btnResendOtp = findViewById(R.id.btn_resend_otp);
        btnConfirmOtp = findViewById(R.id.btn_confirm_otp);
        btnBackToLogin = findViewById(R.id.btn_back_to_login);

        // Long click vào tiêu đề app để mở dialog cấu hình IP
        TextView tvAppTitle = findViewById(R.id.tv_app_title);
        if (tvAppTitle != null) {
            tvAppTitle.setOnLongClickListener(v -> {
                showIpConfigDialog();
                return true;
            });
        }
    }

    private void setupInputListeners() {
        etUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilUsername.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilPassword.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etOtp.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilOtp.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        viewModel.getLoading().observe(this, loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            btnLogin.setEnabled(!isLoading);
            btnLogin.setText(isLoading ? "Đang xác thực..." : "Đăng nhập");
            etUsername.setEnabled(!isLoading);
            etPassword.setEnabled(!isLoading);

            btnConfirmOtp.setEnabled(!isLoading);
            btnConfirmOtp.setText(isLoading ? "Đang xác thực..." : "Xác nhận mã OTP");
            etOtp.setEnabled(!isLoading);
            btnBackToLogin.setEnabled(!isLoading);
        });

        viewModel.getError().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                viewModel.clearError();
            }
        });

        viewModel.getLoginSuccess().observe(this, user -> {
            if (user != null) {
                viewModel.clearLoginSuccess();
                Toast.makeText(this,
                        "Đăng nhập thành công",
                        Toast.LENGTH_SHORT).show();
                navigateToTables();
            }
        });

        // OTP Observers
        viewModel.getRequiresOtp().observe(this, requiresOtp -> {
            if (Boolean.TRUE.equals(requiresOtp)) {
                layoutCredentials.setVisibility(View.GONE);
                layoutOtp.setVisibility(View.VISIBLE);
                startResendTimer();
            } else {
                layoutCredentials.setVisibility(View.VISIBLE);
                layoutOtp.setVisibility(View.GONE);
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                }
            }
        });

        viewModel.getOtpUserId().observe(this, userId -> {
            if (userId != null) {
                otpUserId = userId;
            }
        });

        viewModel.getOtpInfoMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                tvOtpDescription.setText(msg);
            }
        });

        viewModel.getOtpResendSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(this, "Mã OTP mới đã được gửi thành công", Toast.LENGTH_SHORT).show();
                viewModel.clearOtpResendSuccess();
            }
        });

        // Click Listeners
        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString();

            boolean isValid = true;

            if (username.isEmpty()) {
                tilUsername.setError("Tên đăng nhập không được để trống");
                isValid = false;
            }

            if (password.isEmpty()) {
                tilPassword.setError("Mật khẩu không được để trống");
                isValid = false;
            }

            if (isValid) {
                viewModel.login(username, password);
            }
        });

        btnConfirmOtp.setOnClickListener(v -> {
            String otpCode = etOtp.getText().toString().trim();
            if (otpCode.length() != 6) {
                tilOtp.setError("Mã OTP phải có đúng 6 chữ số");
                return;
            }
            viewModel.verifyOtp(otpUserId, otpCode);
        });

        btnResendOtp.setOnClickListener(v -> {
            viewModel.resendOtp(otpUserId);
            startResendTimer();
        });

        btnBackToLogin.setOnClickListener(v -> {
            viewModel.resetRequiresOtp();
            etOtp.setText("");
        });
    }

    private void startResendTimer() {
        btnResendOtp.setEnabled(false);
        btnResendOtp.setTextColor(getResources().getColor(R.color.text_soft));
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                btnResendOtp.setText("Gửi lại mã (" + (millisUntilFinished / 1000) + "s)");
            }

            @Override
            public void onFinish() {
                btnResendOtp.setEnabled(true);
                btnResendOtp.setTextColor(getResources().getColor(R.color.accent));
                btnResendOtp.setText("Gửi lại mã");
            }
        }.start();
    }

    private void navigateToTables() {
        Intent intent = new Intent(this, TableActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void showIpConfigDialog() {
        String currentIp = com.example.cafe_manager.data.remote.ApiClient.getServerIp(this);
        
        EditText etIp = new EditText(this);
        etIp.setText(currentIp);
        etIp.setHint("Ví dụ: 192.168.1.46:8080");
        etIp.setSingleLine(true);
        
        int paddingPx = (int) (16 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        container.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        container.addView(etIp);

        new android.app.AlertDialog.Builder(this)
                .setTitle("Cấu hình IP máy chủ")
                .setMessage("Nhập địa chỉ IP và cổng của máy tính chạy Backend:")
                .setView(container)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newIp = etIp.getText().toString().trim();
                    if (!newIp.isEmpty()) {
                        com.example.cafe_manager.data.remote.ApiClient.setServerIp(this, newIp);
                        Toast.makeText(this, "Đã cập nhật IP máy chủ thành: " + newIp, Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        super.onDestroy();
    }
}
