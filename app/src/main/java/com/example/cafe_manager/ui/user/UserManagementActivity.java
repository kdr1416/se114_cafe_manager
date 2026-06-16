package com.example.cafe_manager.ui.user;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.data.local.entity.UserEntity;
import com.example.cafe_manager.manager.SessionManager;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.PermissionUtils;
import com.example.cafe_manager.viewmodel.UserManagementViewModel;

import java.util.ArrayList;
import java.util.List;

public class UserManagementActivity extends AppCompatActivity {

    private UserManagementViewModel viewModel;
    private UserAdapter adapter;
    private RecyclerView rvUsers;
    private View emptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!PermissionUtils.requireRole(this, Constants.ROLE_ADMIN, Constants.ROLE_MANAGER)) return;

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_management);

        setupTopBar();
        setupRecyclerView();
        setupViewModel();
    }

    private void setupTopBar() {
        View topBar = findViewById(R.id.top_bar);
        TextView title = topBar.findViewById(R.id.tv_title);
        TextView caption = topBar.findViewById(R.id.tv_caption);
        View btnBack = topBar.findViewById(R.id.btn_back);
        ImageButton btnRight = topBar.findViewById(R.id.btn_right);

        title.setText("Quản lý tài khoản");
        caption.setText("Thêm, sửa, khóa tài khoản");
        btnBack.setOnClickListener(v -> finish());

        btnRight.setVisibility(View.VISIBLE);
        btnRight.setImageResource(R.drawable.ic_plus);
        btnRight.setOnClickListener(v -> showUserDialog(null));
    }

    private void setupRecyclerView() {
        rvUsers = findViewById(R.id.rv_users);
        emptyState = findViewById(R.id.empty_state);
        TextView msg = emptyState.findViewById(R.id.tv_empty_message);
        msg.setText("Chưa có tài khoản nào.");

        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserAdapter(new UserAdapter.OnActionListener() {
            @Override
            public void onEdit(UserEntity user) {
                showUserDialog(user);
            }

            @Override
            public void onToggleActive(UserEntity user) {
                viewModel.toggleActive(user.getUserId(), !user.isActive());
            }

            @Override
            public void onResetPassword(UserEntity user) {
                showResetPasswordDialog(user.getUserId());
            }
        });
        rvUsers.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(UserManagementViewModel.class);

        viewModel.getUsers().observe(this, list -> {
            adapter.submitList(list);
            boolean empty = list == null || list.isEmpty();
            rvUsers.setVisibility(empty ? View.GONE : View.VISIBLE);
            emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        });

        viewModel.getMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                viewModel.clearMessage();
            }
        });
    }

    private void showUserDialog(@Nullable UserEntity existing) {
        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_user_form, null);

        EditText etUsername = view.findViewById(R.id.et_username);
        EditText etFullName = view.findViewById(R.id.et_full_name);
        EditText etPhone = view.findViewById(R.id.et_phone);
        Spinner spinnerRole = view.findViewById(R.id.spinner_role);
        View labelPassword = view.findViewById(R.id.label_password);
        EditText etPassword = view.findViewById(R.id.et_password);

        // Role options depend on current user's role
        String myRole = SessionManager.getInstance(this).getRole();
        List<String> roleOptions = new ArrayList<>();
        roleOptions.add(Constants.ROLE_STAFF);
        if (Constants.ROLE_ADMIN.equals(myRole)) {
            roleOptions.add(Constants.ROLE_MANAGER);
        }

        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                roleOptions
        );
        spinnerRole.setAdapter(roleAdapter);

        if (existing != null) {
            etUsername.setText(existing.getUsername());
            etUsername.setEnabled(false);
            etFullName.setText(existing.getFullName());
            etPhone.setText(existing.getPhone());

            // Select current role in spinner
            int roleIdx = roleOptions.indexOf(existing.getRole());
            if (roleIdx >= 0) spinnerRole.setSelection(roleIdx);

            // Hide password field in edit mode
            labelPassword.setVisibility(View.GONE);
            etPassword.setVisibility(View.GONE);
        }

        String title = existing == null ? "Tạo tài khoản" : "Sửa tài khoản";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(view)
                .setPositiveButton("Lưu", (d, w) -> {
                    String username = etUsername.getText().toString().trim();
                    String fullName = etFullName.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();
                    int rolePos = spinnerRole.getSelectedItemPosition();
                    String role = roleOptions.get(rolePos);
                    String password = etPassword.getText().toString();

                    if (username.isEmpty()) {
                        Toast.makeText(this, "Tên đăng nhập không được rỗng", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (fullName.isEmpty()) {
                        Toast.makeText(this, "Họ tên không được rỗng", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (existing == null) {
                        if (password.length() < 6) {
                            Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        viewModel.createUser(username, fullName, phone, role, password);
                    } else {
                        existing.setFullName(fullName);
                        existing.setPhone(phone);
                        existing.setRole(role);
                        viewModel.updateUser(existing);
                    }
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void showResetPasswordDialog(int userId) {
        EditText etPassword = new EditText(this);
        etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etPassword.setHint("Mật khẩu mới");
        etPassword.setBackground(getDrawable(R.drawable.bg_input));
        int pad = getResources().getDimensionPixelSize(R.dimen.spacing_lg);
        etPassword.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(this)
                .setTitle("Đặt lại mật khẩu")
                .setView(etPassword)
                .setPositiveButton("Lưu", (d, w) -> {
                    String newPassword = etPassword.getText().toString();
                    if (newPassword.length() < 6) {
                        Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    viewModel.resetPassword(userId, newPassword);
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }
}
