package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;


import com.example.cafe_manager.data.local.entity.UserEntity;
import com.example.cafe_manager.data.repository.AuthRepository;
import com.example.cafe_manager.manager.SessionManager;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.RepositoryCallback;

import java.util.List;

public class UserManagementViewModel extends AndroidViewModel {

    private final AuthRepository authRepository;
    private final LiveData<List<UserEntity>> usersLive;
    private final MutableLiveData<String> messageLive = new MutableLiveData<>();

    public UserManagementViewModel(@NonNull Application application) {
        super(application);
        authRepository = AuthRepository.getInstance(application);
        SessionManager sessionManager = SessionManager.getInstance(application);

        String role = sessionManager.getRole();
        usersLive = androidx.lifecycle.Transformations.map(authRepository.getAllUsers(), users -> {
            if (users == null) {
                return new java.util.ArrayList<>();
            }
            if (Constants.ROLE_ADMIN.equals(role)) {
                return users;
            } else {
                List<UserEntity> staffOnly = new java.util.ArrayList<>();
                for (UserEntity u : users) {
                    if (Constants.ROLE_STAFF.equals(u.getRole())) {
                        staffOnly.add(u);
                    }
                }
                return staffOnly;
            }
        });

        // Trigger initial API load
        authRepository.refreshAllUsers();
    }

    public LiveData<List<UserEntity>> getUsers() {
        return usersLive;
    }

    public MutableLiveData<String> getMessage() {
        return messageLive;
    }

    public void clearMessage() {
        messageLive.setValue(null);
    }

    public void createUser(String username, String fullName, String phone,
                           String role, String password) {
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setRole(role);

        authRepository.createUser(user, password, new RepositoryCallback<Long>() {
            @Override
            public void onSuccess(Long result) {
                messageLive.setValue("Tạo tài khoản thành công");
            }

            @Override
            public void onError(Exception e) {
                messageLive.setValue(e.getMessage());
            }
        });
    }

    public void updateUser(UserEntity user) {
        authRepository.updateUser(user, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                messageLive.setValue("Cập nhật thành công");
            }

            @Override
            public void onError(Exception e) {
                messageLive.setValue(e.getMessage());
            }
        });
    }

    public void toggleActive(int userId, boolean newActive) {
        authRepository.setActive(userId, newActive, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                messageLive.setValue("Đã cập nhật trạng thái");
            }

            @Override
            public void onError(Exception e) {
                messageLive.setValue(e.getMessage());
            }
        });
    }

    public void resetPassword(int userId, String newPassword) {
        authRepository.resetPassword(userId, newPassword, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                messageLive.setValue("Đã đặt lại mật khẩu");
            }

            @Override
            public void onError(Exception e) {
                messageLive.setValue(e.getMessage());
            }
        });
    }
}
