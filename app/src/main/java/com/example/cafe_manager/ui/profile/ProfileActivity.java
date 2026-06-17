package com.example.cafe_manager.ui.profile;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cafe_manager.R;
import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.entity.UserEntity;
import com.example.cafe_manager.manager.CartManager;
import com.example.cafe_manager.manager.SessionManager;
import com.example.cafe_manager.ui.auth.LoginActivity;
import com.example.cafe_manager.util.AppExecutors;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvAvatar;
    private TextView tvFullName;
    private TextView tvUsername;
    private TextView tvRole;
    private TextView tvPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        setupTopBar();
        bindViews();
        loadProfile();
    }

    private void setupTopBar() {
        View topBar = findViewById(R.id.top_bar);
        TextView title = topBar.findViewById(R.id.tv_title);
        TextView caption = topBar.findViewById(R.id.tv_caption);
        View btnBack = topBar.findViewById(R.id.btn_back);
        View btnRight = topBar.findViewById(R.id.btn_right);

        title.setText("Hồ sơ cá nhân");
        caption.setText("Thông tin tài khoản của bạn");
        btnBack.setOnClickListener(v -> finish());
        btnRight.setVisibility(View.GONE);
    }

    private void bindViews() {
        tvAvatar = findViewById(R.id.tv_avatar);
        tvFullName = findViewById(R.id.tv_full_name);
        tvUsername = findViewById(R.id.tv_username);
        tvRole = findViewById(R.id.tv_role);
        tvPhone = findViewById(R.id.tv_phone);

        Button btnChangePassword = findViewById(R.id.btn_change_password);
        Button btnLogout = findViewById(R.id.btn_logout);

        btnChangePassword.setOnClickListener(v ->
                startActivity(new Intent(this, ChangePasswordActivity.class)));
        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void loadProfile() {
        SessionManager session = SessionManager.getInstance(this);

        // Show from session immediately
        tvFullName.setText(session.getFullName());
        tvUsername.setText("@" + session.getUsername());
        tvRole.setText(session.getRole());

        String fullName = session.getFullName();
        if (fullName != null && !fullName.isEmpty()) {
            tvAvatar.setText(String.valueOf(fullName.charAt(0)).toUpperCase());
        }

        // Load phone from DB (not stored in session)
        AppExecutors.getInstance().diskIO().execute(() -> {
            UserEntity user = AppDatabase.getInstance(this).userDao().getById(session.getUserId());
            if (user != null) {
                AppExecutors.getInstance().mainThread().execute(() -> {
                    String phone = user.getPhone();
                    tvPhone.setText(phone != null && !phone.isEmpty() ? phone : "Chưa cập nhật");
                    // Also refresh other fields from DB
                    tvFullName.setText(user.getFullName());
                    tvRole.setText(user.getRole());
                });
            }
        });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng xuất?")
                .setMessage("Bạn sẽ phải đăng nhập lại để tiếp tục.")
                .setPositiveButton("Đăng xuất", (d, w) -> performLogout())
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void performLogout() {
        SessionManager.getInstance(this).logout();
        CartManager.getInstance().clearCart();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
