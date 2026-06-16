package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.local.entity.UserEntity;
import com.example.cafe_manager.data.repository.AuthRepository;
import com.example.cafe_manager.manager.SessionManager;
import com.example.cafe_manager.util.RepositoryCallback;

public class AuthViewModel extends AndroidViewModel {

    private final AuthRepository authRepository;
    private final SessionManager sessionManager;

    private final MutableLiveData<Boolean> loadingLive = new MutableLiveData<>(false);
    private final MutableLiveData<UserEntity> loginSuccessLive = new MutableLiveData<>();
    private final MutableLiveData<String> errorLive = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        authRepository = new AuthRepository(application);
        sessionManager = SessionManager.getInstance(application);
    }

    public MutableLiveData<Boolean> getLoading() {
        return loadingLive;
    }

    public MutableLiveData<UserEntity> getLoginSuccess() {
        return loginSuccessLive;
    }

    public MutableLiveData<String> getError() {
        return errorLive;
    }

    public void clearError() {
        errorLive.setValue(null);
    }

    public void clearLoginSuccess() {
        loginSuccessLive.setValue(null);
    }

    public void login(String username, String password) {
        loadingLive.setValue(true);
        authRepository.login(username, password, new RepositoryCallback<UserEntity>() {
            @Override
            public void onSuccess(UserEntity user) {
                loadingLive.setValue(false);
                sessionManager.saveLoginSession(user);
                loginSuccessLive.setValue(user);
            }

            @Override
            public void onError(Exception e) {
                loadingLive.setValue(false);
                errorLive.setValue(e.getMessage());
            }
        });
    }
}
