package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.local.AppDatabase;
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
        authRepository = new AuthRepository(application);
        SessionManager sessionManager = SessionManager.getInstance(application);
        AppDatabase db = AppDatabase.getInstance(application);

        String role = sessionManager.getRole();
        if (Constants.ROLE_ADMIN.equals(role)) {
            usersLive = db.userDao().getAllUsers();
        } else {
            usersLive = db.userDao().getUsersByRole(Constants.ROLE_STAFF);
        }
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
