package com.example.cafe_manager.ui.auth;

import android.content.Intent;
import android.os.Bundle;
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

public class LoginActivity extends AppCompatActivity {

    private AuthViewModel viewModel;

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
    }

    private void bindViews() {
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
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
            String username = etUsername.getText().toString();
            String password = etPassword.getText().toString();
            viewModel.login(username, password);
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
