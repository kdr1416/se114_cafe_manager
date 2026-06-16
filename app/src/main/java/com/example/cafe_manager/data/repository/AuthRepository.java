package com.example.cafe_manager.data.repository;

import android.content.Context;

import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.dao.AuditLogDao;
import com.example.cafe_manager.data.local.dao.UserDao;
import com.example.cafe_manager.data.local.entity.AuditLogEntity;
import com.example.cafe_manager.data.local.entity.UserEntity;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.PasswordUtils;
import com.example.cafe_manager.util.RepositoryCallback;

public class AuthRepository {

    private final UserDao userDao;
    private final AuditLogDao auditLogDao;

    public AuthRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.userDao = db.userDao();
        this.auditLogDao = db.auditLogDao();
    }

    public void login(String username, String password, RepositoryCallback<UserEntity> callback) {
        String trimmedUsername = username != null ? username.trim() : "";
        String trimmedPassword = password != null ? password.trim() : "";

        if (trimmedUsername.isEmpty()) {
            callback.onError(new Exception("Vui lòng nhập tên đăng nhập."));
            return;
        }
        if (trimmedPassword.isEmpty()) {
            callback.onError(new Exception("Vui lòng nhập mật khẩu."));
            return;
        }

        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                UserEntity user = userDao.getByUsername(trimmedUsername);
                if (user == null) {
                    postError(callback, "Tài khoản không tồn tại.");
                    return;
                }
                if (!user.isActive()) {
                    postError(callback, "Tài khoản đã bị khóa.");
                    return;
                }
                String hashed = PasswordUtils.hashPassword(trimmedPassword);
                if (!hashed.equals(user.getPasswordHash())) {
                    postError(callback, "Mật khẩu không đúng.");
                    return;
                }

                userDao.updateLastLogin(user.getUserId(), System.currentTimeMillis());
                logAudit(user.getUserId(), Constants.ACTION_LOGIN, "USER",
                        String.valueOf(user.getUserId()), "Đăng nhập thành công");

                AppExecutors.getInstance().mainThread().execute(() -> callback.onSuccess(user));
            } catch (Exception e) {
                postError(callback, e.getMessage());
            }
        });
    }

    public void createUser(UserEntity user, String plainPassword, RepositoryCallback<Long> callback) {
        String username = user.getUsername() != null ? user.getUsername().trim() : "";
        if (username.isEmpty()) {
            callback.onError(new Exception("Tên đăng nhập không được để trống."));
            return;
        }
        if (plainPassword == null || plainPassword.length() < 6) {
            callback.onError(new Exception("Mật khẩu phải có ít nhất 6 ký tự."));
            return;
        }
        String role = user.getRole();
        if (!Constants.ROLE_ADMIN.equals(role)
                && !Constants.ROLE_MANAGER.equals(role)
                && !Constants.ROLE_STAFF.equals(role)) {
            callback.onError(new Exception("Vai trò không hợp lệ."));
            return;
        }

        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                UserEntity existing = userDao.getByUsername(username);
                if (existing != null) {
                    postError(callback, "Tên đăng nhập đã tồn tại.");
                    return;
                }

                user.setUsername(username);
                user.setPasswordHash(PasswordUtils.hashPassword(plainPassword));
                long now = System.currentTimeMillis();
                user.setCreatedAt(now);
                user.setUpdatedAt(now);
                user.setActive(true);

                long id = userDao.insert(user);
                logAudit(0, Constants.ACTION_CREATE_USER, "USER",
                        String.valueOf(id), "Tạo tài khoản: " + username);

                AppExecutors.getInstance().mainThread().execute(() -> callback.onSuccess(id));
            } catch (Exception e) {
                postError(callback, e.getMessage());
            }
        });
    }

    public void updateUser(UserEntity user, RepositoryCallback<Void> callback) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                user.setUpdatedAt(System.currentTimeMillis());
                userDao.update(user);
                logAudit(0, Constants.ACTION_UPDATE_USER, "USER",
                        String.valueOf(user.getUserId()),
                        "Cập nhật tài khoản: " + user.getUsername());

                AppExecutors.getInstance().mainThread().execute(() -> callback.onSuccess(null));
            } catch (Exception e) {
                postError(callback, e.getMessage());
            }
        });
    }

    public void resetPassword(int userId, String newPassword, RepositoryCallback<Void> callback) {
        if (newPassword == null || newPassword.length() < 6) {
            callback.onError(new Exception("Mật khẩu phải có ít nhất 6 ký tự."));
            return;
        }

        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                String hashed = PasswordUtils.hashPassword(newPassword);
                userDao.updatePassword(userId, hashed);
                logAudit(0, Constants.ACTION_RESET_PASSWORD, "USER",
                        String.valueOf(userId), "Đặt lại mật khẩu");

                AppExecutors.getInstance().mainThread().execute(() -> callback.onSuccess(null));
            } catch (Exception e) {
                postError(callback, e.getMessage());
            }
        });
    }

    public void setActive(int userId, boolean isActive, RepositoryCallback<Void> callback) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                userDao.setActive(userId, isActive);
                String desc = isActive ? "Mở khóa tài khoản" : "Khóa tài khoản";
                logAudit(0, Constants.ACTION_LOCK_USER, "USER",
                        String.valueOf(userId), desc);

                AppExecutors.getInstance().mainThread().execute(() -> callback.onSuccess(null));
            } catch (Exception e) {
                postError(callback, e.getMessage());
            }
        });
    }

    // ── Helper ────────────────────────────────────────────────────

    private void logAudit(int userId, String action, String targetType,
                          String targetId, String description) {
        AuditLogEntity log = new AuditLogEntity();
        log.setUserId(userId);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDescription(description);
        log.setCreatedAt(System.currentTimeMillis());
        auditLogDao.insert(log);
    }

    private <T> void postError(RepositoryCallback<T> callback, String message) {
        AppExecutors.getInstance().mainThread().execute(
                () -> callback.onError(new Exception(message)));
    }
}
