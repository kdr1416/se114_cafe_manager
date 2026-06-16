package com.example.cafe_manager.ui.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cafe_manager.R;
import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.dao.UserDao;
import com.example.cafe_manager.data.local.entity.UserEntity;
import com.example.cafe_manager.manager.SessionManager;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.PasswordUtils;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText etOldPassword;
    private EditText etNewPassword;
    private EditText etConfirmPassword;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_password);

        setupTopBar();
        bindViews();
    }

    private void setupTopBar() {
        View topBar = findViewById(R.id.top_bar);
        TextView title = topBar.findViewById(R.id.tv_title);
        TextView caption = topBar.findViewById(R.id.tv_caption);
        View btnBack = topBar.findViewById(R.id.btn_back);
        View btnRight = topBar.findViewById(R.id.btn_right);

        title.setText("Đổi mật khẩu");
        caption.setText("Nhập mật khẩu cũ và mới");
        btnBack.setOnClickListener(v -> finish());
        btnRight.setVisibility(View.GONE);
    }

    private void bindViews() {
        etOldPassword = findViewById(R.id.et_old_password);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnSave = findViewById(R.id.btn_save);

        btnSave.setOnClickListener(v -> onSave());
    }

    private void onSave() {
        String oldPassword = etOldPassword.getText().toString();
        String newPassword = etNewPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ các trường", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPassword.length() < 6) {
            Toast.makeText(this, "Mật khẩu mới phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);

        int userId = SessionManager.getInstance(this).getUserId();
        UserDao userDao = AppDatabase.getInstance(this).userDao();

        AppExecutors.getInstance().diskIO().execute(() -> {
            UserEntity user = userDao.getById(userId);
            if (user == null) {
                AppExecutors.getInstance().mainThread().execute(() -> {
                    Toast.makeText(this, "Không tìm thấy tài khoản", Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                });
                return;
            }

            String oldHash = PasswordUtils.hashPassword(oldPassword);
            if (!oldHash.equals(user.getPasswordHash())) {
                AppExecutors.getInstance().mainThread().execute(() -> {
                    Toast.makeText(this, "Mật khẩu hiện tại không đúng", Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                });
                return;
            }

            String newHash = PasswordUtils.hashPassword(newPassword);
            userDao.updatePassword(userId, newHash);

            AppExecutors.getInstance().mainThread().execute(() -> {
                Toast.makeText(this, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }
}
