package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.local.entity.UserEntity;
import com.example.cafe_manager.data.repository.AuthRepository;
import com.example.cafe_manager.util.RepositoryCallback;

/**
 * ViewModel xử lý thông tin cá nhân và đổi mật khẩu.
 */
public class ProfileViewModel extends AndroidViewModel {

    private final AuthRepository authRepository;
    private final MutableLiveData<UserEntity> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorLive = new MutableLiveData<>();
    private final MutableLiveData<Boolean> changePasswordSuccess = new MutableLiveData<>();

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        this.authRepository = AuthRepository.getInstance(application);
    }

    public LiveData<UserEntity> getUserLiveData() {
        return userLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return errorLive;
    }

    public LiveData<Boolean> getChangePasswordSuccess() {
        return changePasswordSuccess;
    }

    public void clearError() {
        errorLive.setValue(null);
    }

    public void clearChangePasswordSuccess() {
        changePasswordSuccess.setValue(null);
    }

    public void loadUserProfile(int userId) {
        isLoading.setValue(true);
        authRepository.getUserDetail(userId, new RepositoryCallback<UserEntity>() {
            @Override
            public void onSuccess(UserEntity user) {
                isLoading.setValue(false);
                userLiveData.setValue(user);
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                errorLive.setValue(e.getMessage());
            }
        });
    }

    public void changePassword(int userId, String oldPassword, String newPassword) {
        isLoading.setValue(true);
        authRepository.changePassword(userId, oldPassword, newPassword, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                isLoading.setValue(false);
                changePasswordSuccess.setValue(true);
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                errorLive.setValue(e.getMessage());
            }
        });
    }
}
