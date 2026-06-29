package com.example.cafe_manager.data.repository;

import android.content.Context;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.local.entity.UserEntity;
import com.example.cafe_manager.data.remote.ApiClient;
import com.example.cafe_manager.data.remote.AuthApiService;
import com.example.cafe_manager.data.remote.UserApiService;
import com.example.cafe_manager.data.remote.AuditLogApiService;
import com.example.cafe_manager.data.remote.UserResponse;
import com.example.cafe_manager.data.remote.LoginRequest;
import com.example.cafe_manager.data.remote.LoginResponse;
import com.example.cafe_manager.data.remote.CreateUserRequest;
import com.example.cafe_manager.data.remote.UpdateUserRequest;
import com.example.cafe_manager.data.remote.ChangePasswordRequest;
import com.example.cafe_manager.data.remote.UpdateUserStatusRequest;
import com.example.cafe_manager.data.remote.CreateAuditLogRequest;
import com.example.cafe_manager.data.remote.AuditLogResponse;
import com.example.cafe_manager.data.remote.NetworkException;
import com.example.cafe_manager.manager.SessionManager;
import com.example.cafe_manager.data.remote.VerifyOtpRequest;
import com.example.cafe_manager.data.remote.ResendOtpRequest;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.RepositoryCallback;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository quản lý xác thực và thông tin người dùng qua REST APIs.
 */
public class AuthRepository {

    private static AuthRepository instance;

    private final Context context;
    private final AuthApiService authApiService;
    private final UserApiService userApiService;
    private final AuditLogApiService auditLogApiService;
    private final AppExecutors appExecutors;
    private final MutableLiveData<List<UserEntity>> allUsers;
    private final MutableLiveData<Boolean> isLoading;

    public static synchronized AuthRepository getInstance(Context context) {
        if (instance == null) {
            instance = new AuthRepository(context);
        }
        return instance;
    }

    @Deprecated
    public AuthRepository(Context context) {
        this.context = context.getApplicationContext();
        ApiClient apiClient = ApiClient.getInstance(context);
        this.authApiService = apiClient.getService(AuthApiService.class);
        this.userApiService = apiClient.getService(UserApiService.class);
        this.auditLogApiService = apiClient.getService(AuditLogApiService.class);
        this.appExecutors = AppExecutors.getInstance();
        this.allUsers = new MutableLiveData<>();
        this.isLoading = new MutableLiveData<>(false);
        
        // Chỉ tải lại user nếu đã đăng nhập và là ADMIN/MANAGER
        SessionManager session = SessionManager.getInstance(this.context);
        if (session.isLoggedIn() && (session.isAdmin() || session.isManager())) {
            refreshAllUsers();
        }
    }

    public LiveData<List<UserEntity>> getAllUsers() {
        return allUsers;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    /**
     * Tải lại danh sách toàn bộ người dùng từ backend.
     */
    public void refreshAllUsers() {
        isLoading.postValue(true);
        userApiService.getAllUsers().enqueue(new Callback<List<UserResponse>>() {
            @Override
            public void onResponse(Call<List<UserResponse>> call, Response<List<UserResponse>> response) {
                isLoading.postValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<UserResponse> responses = response.body();
                    List<UserEntity> entities = new ArrayList<>();
                    for (UserResponse r : responses) {
                        entities.add(mapResponseToEntity(r));
                    }
                    allUsers.postValue(entities);
                } else {
                    showError(parseError(response));
                }
            }

            @Override
            public void onFailure(Call<List<UserResponse>> call, Throwable t) {
                isLoading.postValue(false);
                showError(new NetworkException("Không có kết nối mạng", t));
            }
        });
    }

    public void getActiveUsers(RepositoryCallback<List<UserEntity>> callback) {
        userApiService.getAllUsers().enqueue(new Callback<List<UserResponse>>() {
            @Override
            public void onResponse(Call<List<UserResponse>> call, Response<List<UserResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<UserEntity> activeUsers = new ArrayList<>();
                    for (UserResponse r : response.body()) {
                        if (r.isActive()) {
                            activeUsers.add(mapResponseToEntity(r));
                        }
                    }
                    appExecutors.mainThread().execute(() -> callback.onSuccess(activeUsers));
                } else {
                    appExecutors.mainThread().execute(() -> callback.onError(new Exception("Không thể tải danh sách nhân viên: " + response.code())));
                }
            }

            @Override
            public void onFailure(Call<List<UserResponse>> call, Throwable t) {
                appExecutors.mainThread().execute(() -> callback.onError(new Exception("Không có kết nối mạng", t)));
            }
        });
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

        isLoading.postValue(true);
        LoginRequest req = new LoginRequest(trimmedUsername, trimmedPassword);
        authApiService.login(req).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                isLoading.postValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    if (Boolean.TRUE.equals(loginResponse.getRequiresVerification())) {
                        String msg = loginResponse.getMessage() != null ? loginResponse.getMessage() : "Yêu cầu xác thực OTP.";
                        appExecutors.mainThread().execute(() -> callback.onError(new Exception("OTP_REQUIRED:" + loginResponse.getUserId() + ":" + msg)));
                        return;
                    }

                    UserEntity user = new UserEntity();
                    user.setUserId(loginResponse.getUserId());
                    user.setUsername(trimmedUsername);
                    user.setFullName(loginResponse.getFullName());
                    user.setRole(loginResponse.getRole());
                    user.setActive(true);

                    SessionManager sessionManager = SessionManager.getInstance(context);
                    sessionManager.saveToken(loginResponse.getToken());
                    sessionManager.saveLoginSession(user);

                    logAudit(user.getUserId(), Constants.ACTION_LOGIN, "USER", String.valueOf(user.getUserId()), "Đăng nhập thành công");

                    // Tải lại list user nếu đăng nhập với quyền ADMIN/MANAGER
                    if (sessionManager.isAdmin() || sessionManager.isManager()) {
                        refreshAllUsers();
                    }

                    appExecutors.mainThread().execute(() -> callback.onSuccess(user));
                } else {
                    Exception e = parseError(response);
                    showError(e);
                    appExecutors.mainThread().execute(() -> callback.onError(e));
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                isLoading.postValue(false);
                Exception e = new NetworkException("Không có kết nối mạng", t);
                showError(e);
                appExecutors.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void verifyOtp(int userId, String otpCode, RepositoryCallback<UserEntity> callback) {
        isLoading.postValue(true);
        VerifyOtpRequest req = new VerifyOtpRequest(userId, otpCode);
        authApiService.verifyOtp(req).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                isLoading.postValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    UserEntity user = new UserEntity();
                    user.setUserId(loginResponse.getUserId());
                    user.setUsername(""); // Temporarily empty, SessionManager accepts it
                    user.setFullName(loginResponse.getFullName());
                    user.setRole(loginResponse.getRole());
                    user.setActive(true);

                    SessionManager sessionManager = SessionManager.getInstance(context);
                    sessionManager.saveToken(loginResponse.getToken());
                    sessionManager.saveLoginSession(user);

                    logAudit(user.getUserId(), Constants.ACTION_LOGIN, "USER", String.valueOf(user.getUserId()), "Xác thực OTP thành công");

                    if (sessionManager.isAdmin() || sessionManager.isManager()) {
                        refreshAllUsers();
                    }

                    appExecutors.mainThread().execute(() -> callback.onSuccess(user));
                } else {
                    Exception e = parseError(response);
                    showError(e);
                    appExecutors.mainThread().execute(() -> callback.onError(e));
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                isLoading.postValue(false);
                Exception e = new NetworkException("Không có kết nối mạng", t);
                showError(e);
                appExecutors.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void resendOtp(int userId, RepositoryCallback<Void> callback) {
        ResendOtpRequest req = new ResendOtpRequest(userId);
        authApiService.resendOtp(req).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    appExecutors.mainThread().execute(() -> callback.onSuccess(null));
                } else {
                    Exception e = parseError(response);
                    showError(e);
                    appExecutors.mainThread().execute(() -> callback.onError(e));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Exception e = new NetworkException("Không có kết nối mạng", t);
                showError(e);
                appExecutors.mainThread().execute(() -> callback.onError(e));
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

        isLoading.postValue(true);
        CreateUserRequest req = new CreateUserRequest(username, plainPassword, user.getFullName(), user.getPhone(), role);
        userApiService.createUser(req).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                isLoading.postValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    UserResponse newUser = response.body();
                    logAudit(0, Constants.ACTION_CREATE_USER, "USER", String.valueOf(newUser.getUserId()), "Tạo tài khoản: " + username);
                    refreshAllUsers();
                    appExecutors.mainThread().execute(() -> callback.onSuccess((long) newUser.getUserId()));
                } else {
                    Exception e = parseError(response);
                    showError(e);
                    appExecutors.mainThread().execute(() -> callback.onError(e));
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                isLoading.postValue(false);
                Exception e = new NetworkException("Không có kết nối mạng", t);
                showError(e);
                appExecutors.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void updateUser(UserEntity user, RepositoryCallback<Void> callback) {
        isLoading.postValue(true);
        UpdateUserRequest req = new UpdateUserRequest(user.getFullName(), user.getPhone(), user.getRole());
        userApiService.updateUser(user.getUserId(), req).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                isLoading.postValue(false);
                if (response.isSuccessful()) {
                    logAudit(0, Constants.ACTION_UPDATE_USER, "USER", String.valueOf(user.getUserId()), "Cập nhật tài khoản: " + user.getUsername());
                    refreshAllUsers();
                    appExecutors.mainThread().execute(() -> callback.onSuccess(null));
                } else {
                    Exception e = parseError(response);
                    showError(e);
                    appExecutors.mainThread().execute(() -> callback.onError(e));
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                isLoading.postValue(false);
                Exception e = new NetworkException("Không có kết nối mạng", t);
                showError(e);
                appExecutors.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void resetPassword(int userId, String newPassword, RepositoryCallback<Void> callback) {
        if (newPassword == null || newPassword.length() < 6) {
            callback.onError(new Exception("Mật khẩu phải có ít nhất 6 ký tự."));
            return;
        }

        isLoading.postValue(true);
        ChangePasswordRequest req = new ChangePasswordRequest(null, newPassword);
        userApiService.changePassword(userId, req).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                isLoading.postValue(false);
                if (response.isSuccessful()) {
                    logAudit(0, Constants.ACTION_RESET_PASSWORD, "USER", String.valueOf(userId), "Đặt lại mật khẩu");
                    refreshAllUsers();
                    appExecutors.mainThread().execute(() -> callback.onSuccess(null));
                } else {
                    Exception e = parseError(response);
                    showError(e);
                    appExecutors.mainThread().execute(() -> callback.onError(e));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                isLoading.postValue(false);
                Exception e = new NetworkException("Không có kết nối mạng", t);
                showError(e);
                appExecutors.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void setActive(int userId, boolean isActive, RepositoryCallback<Void> callback) {
        isLoading.postValue(true);
        UpdateUserStatusRequest req = new UpdateUserStatusRequest(isActive);
        userApiService.updateUserStatus(userId, req).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                isLoading.postValue(false);
                if (response.isSuccessful()) {
                    String desc = isActive ? "Mở khóa tài khoản" : "Khóa tài khoản";
                    logAudit(0, Constants.ACTION_LOCK_USER, "USER", String.valueOf(userId), desc);
                    refreshAllUsers();
                    appExecutors.mainThread().execute(() -> callback.onSuccess(null));
                } else {
                    Exception e = parseError(response);
                    showError(e);
                    appExecutors.mainThread().execute(() -> callback.onError(e));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                isLoading.postValue(false);
                Exception e = new NetworkException("Không có kết nối mạng", t);
                showError(e);
                appExecutors.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void getUserDetail(int userId, RepositoryCallback<UserEntity> callback) {
        isLoading.postValue(true);
        userApiService.getUserDetail(userId).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                isLoading.postValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    UserEntity user = mapResponseToEntity(response.body());
                    appExecutors.mainThread().execute(() -> callback.onSuccess(user));
                } else {
                    Exception e = parseError(response);
                    showError(e);
                    appExecutors.mainThread().execute(() -> callback.onError(e));
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                isLoading.postValue(false);
                Exception e = new NetworkException("Không có kết nối mạng", t);
                showError(e);
                appExecutors.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void changePassword(int userId, String oldPassword, String newPassword, RepositoryCallback<Void> callback) {
        isLoading.postValue(true);
        ChangePasswordRequest request = new ChangePasswordRequest(oldPassword, newPassword);
        userApiService.changePassword(userId, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                isLoading.postValue(false);
                if (response.isSuccessful()) {
                    logAudit(userId, Constants.ACTION_RESET_PASSWORD, "USER", String.valueOf(userId), "Đổi mật khẩu cá nhân");
                    appExecutors.mainThread().execute(() -> callback.onSuccess(null));
                } else {
                    Exception e = parseError(response);
                    showError(e);
                    appExecutors.mainThread().execute(() -> callback.onError(e));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                isLoading.postValue(false);
                Exception e = new NetworkException("Không có kết nối mạng", t);
                showError(e);
                appExecutors.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    // ── Helpers ──────────────────────────────────────────────────

    private void logAudit(int userId, String action, String targetType, String targetId, String description) {
        CreateAuditLogRequest req = new CreateAuditLogRequest(action, targetType, targetId, description);
        auditLogApiService.createLog(req).enqueue(new Callback<AuditLogResponse>() {
            @Override
            public void onResponse(Call<AuditLogResponse> call, Response<AuditLogResponse> response) {}

            @Override
            public void onFailure(Call<AuditLogResponse> call, Throwable t) {}
        });
    }

    private UserEntity mapResponseToEntity(UserResponse response) {
        if (response == null) return null;
        UserEntity user = new UserEntity();
        user.setUserId(response.getUserId());
        user.setUsername(response.getUsername());
        user.setPasswordHash(response.getPasswordHash());
        user.setFullName(response.getFullName());
        user.setPhone(response.getPhone());
        user.setRole(response.getRole());
        user.setActive(response.isActive());
        user.setCreatedAt(response.getCreatedAt());
        user.setUpdatedAt(response.getUpdatedAt());
        user.setLastLoginAt(response.getLastLoginAt());
        return user;
    }

    private Exception parseError(Response<?> response) {
        if (response == null) return new NetworkException("Không có kết nối mạng");
        switch (response.code()) {
            case 400:
                try {
                    String errorBody = response.errorBody().string();
                    if (errorBody.contains("Tên đăng nhập đã tồn tại")) {
                        return new Exception("Tên đăng nhập đã tồn tại.");
                    }
                    if (errorBody.contains("Mật khẩu không đúng")) {
                        return new Exception("Mật khẩu không đúng.");
                    }
                } catch (Exception e) {
                    // ignore
                }
                return new Exception("Yêu cầu không hợp lệ (400)");
            case 401:
                return new Exception("Tên đăng nhập hoặc mật khẩu không đúng.");
            case 403:
                return new Exception("Không có quyền thực hiện thao tác này (403)");
            case 404:
                return new Exception("Không tìm thấy dữ liệu (404)");
            case 500:
                return new Exception("Lỗi máy chủ (500)");
            default:
                return new Exception("Lỗi hệ thống: " + response.code());
        }
    }

    private void showError(final Exception e) {
        appExecutors.mainThread().execute(() -> 
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show()
        );
    }
}
