package com.example.cafe_manager.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
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
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        viewModel.getLoading().observe(this, loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            btnLogin.setEnabled(!isLoading);
            btnLogin.setText(isLoading ? "Đang xác thực..." : "Đăng nhập");
            etUsername.setEnabled(!isLoading);
            etPassword.setEnabled(!isLoading);
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
    }

    private void navigateToTables() {
        Intent intent = new Intent(this, TableActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
