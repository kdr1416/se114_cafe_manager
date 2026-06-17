package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.local.entity.UserEntity;
import com.example.cafe_manager.data.repository.UserRepository;
import com.example.cafe_manager.manager.SessionManager;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.PasswordUtils;

public class LoginViewModel extends AndroidViewModel {

    private final UserRepository userRepository;
    private final SessionManager sessionManager;

    private final MutableLiveData<Boolean> loadingLive = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> loginSuccessLive = new MutableLiveData<>();
    private final MutableLiveData<String> errorLive = new MutableLiveData<>();

    public LoginViewModel(@NonNull Application application) {
        super(application);
        this.userRepository = new UserRepository(application);
        this.sessionManager = SessionManager.getInstance(application);
    }

    public LiveData<Boolean> getLoading() { return loadingLive; }
    public LiveData<Boolean> getLoginSuccess() { return loginSuccessLive; }
    public LiveData<String> getError() { return errorLive; }

    public void clearError() { errorLive.setValue(null); }
    public void clearLoginSuccess() { loginSuccessLive.setValue(null); }

    public void login(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            errorLive.setValue("Vui lòng nhập tên đăng nhập.");
            return;
        }
        if (password == null || password.isEmpty()) {
            errorLive.setValue("Vui lòng nhập mật khẩu.");
            return;
        }

        final String input = username.trim();
        final String hashed = PasswordUtils.hashPassword(password);

        loadingLive.setValue(true);

        AppExecutors.getInstance().diskIO().execute(() -> {
            final UserEntity user = userRepository.getByUsername(input);

            AppExecutors.getInstance().mainThread().execute(() -> {
                loadingLive.setValue(false);

                if (user == null) {
                    errorLive.setValue("Tài khoản không tồn tại.");
                    return;
                }
                if (!hashed.equals(user.getPasswordHash())) {
                    errorLive.setValue("Mật khẩu không đúng.");
                    return;
                }

                sessionManager.saveSession(user);
                loginSuccessLive.setValue(true);
            });
        });
    }
}
