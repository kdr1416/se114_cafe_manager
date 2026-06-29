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
    
    // OTP LiveData
    private final MutableLiveData<Boolean> requiresOtpLive = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> otpUserIdLive = new MutableLiveData<>();
    private final MutableLiveData<String> otpInfoMessageLive = new MutableLiveData<>();
    private final MutableLiveData<Boolean> otpResendSuccessLive = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        authRepository = AuthRepository.getInstance(application);
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

    public MutableLiveData<Boolean> getRequiresOtp() {
        return requiresOtpLive;
    }

    public MutableLiveData<Integer> getOtpUserId() {
        return otpUserIdLive;
    }

    public MutableLiveData<String> getOtpInfoMessage() {
        return otpInfoMessageLive;
    }

    public MutableLiveData<Boolean> getOtpResendSuccess() {
        return otpResendSuccessLive;
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
                String msg = e.getMessage();
                if (msg != null && msg.startsWith("OTP_REQUIRED:")) {
                    String[] parts = msg.split(":", 3);
                    int userId = Integer.parseInt(parts[1]);
                    String infoMsg = parts[2];
                    otpUserIdLive.setValue(userId);
                    otpInfoMessageLive.setValue(infoMsg);
                    requiresOtpLive.setValue(true);
                } else {
                    errorLive.setValue(msg);
                }
            }
        });
    }

    public void verifyOtp(int userId, String otpCode) {
        loadingLive.setValue(true);
        authRepository.verifyOtp(userId, otpCode, new RepositoryCallback<UserEntity>() {
            @Override
            public void onSuccess(UserEntity user) {
                loadingLive.setValue(false);
                requiresOtpLive.setValue(false);
                loginSuccessLive.setValue(user);
            }

            @Override
            public void onError(Exception e) {
                loadingLive.setValue(false);
                errorLive.setValue(e.getMessage());
            }
        });
    }

    public void resendOtp(int userId) {
        authRepository.resendOtp(userId, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                otpResendSuccessLive.setValue(true);
            }

            @Override
            public void onError(Exception e) {
                errorLive.setValue(e.getMessage());
            }
        });
    }

    public void clearOtpResendSuccess() {
        otpResendSuccessLive.setValue(null);
    }

    public void resetRequiresOtp() {
        requiresOtpLive.setValue(false);
        otpUserIdLive.setValue(null);
        otpInfoMessageLive.setValue(null);
    }
}
