package com.example.cafe_manager.data.repository;

import android.content.Context;
import com.example.cafe_manager.data.remote.ApiClient;
import com.example.cafe_manager.data.remote.AvailabilityApiService;
import com.example.cafe_manager.data.remote.AvailabilityResponse;
import com.example.cafe_manager.data.remote.PublishAvailabilityRequest;
import com.example.cafe_manager.data.remote.SetAvailabilityRequest;
import com.example.cafe_manager.data.remote.ShiftTemplateResponse;
import com.example.cafe_manager.data.remote.UserResponse;
import com.example.cafe_manager.data.remote.WeekLockResponse;
import com.example.cafe_manager.data.remote.ConflictException;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.RepositoryCallback;
import java.util.List;
import retrofit2.Response;

public class AvailabilityRepository {
    private static volatile AvailabilityRepository instance;
    private final AvailabilityApiService apiService;
    private final AppExecutors exec;

    private AvailabilityRepository(Context ctx) {
        this.apiService = ApiClient.getInstance(ctx).getService(AvailabilityApiService.class);
        this.exec = AppExecutors.getInstance();
    }

    public static AvailabilityRepository getInstance(Context ctx) {
        if (instance == null) {
            synchronized (AvailabilityRepository.class) {
                if (instance == null) {
                    instance = new AvailabilityRepository(ctx.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public void getMyAvailability(RepositoryCallback<List<AvailabilityResponse>> callback) {
        exec.diskIO().execute(() -> {
            try {
                Response<List<AvailabilityResponse>> response = apiService.getMyAvailability().execute();
                if (response.isSuccessful() && response.body() != null) {
                    exec.mainThread().execute(() -> callback.onSuccess(response.body()));
                } else {
                    exec.mainThread().execute(() -> callback.onError(new Exception("Lỗi khi tải lịch rảnh: " + response.code())));
                }
            } catch (Exception e) {
                exec.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void setAvailability(SetAvailabilityRequest request, RepositoryCallback<AvailabilityResponse> callback) {
        exec.diskIO().execute(() -> {
            try {
                Response<AvailabilityResponse> response = apiService.setAvailability(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    exec.mainThread().execute(() -> callback.onSuccess(response.body()));
                } else if (response.code() == 409) {
                    exec.mainThread().execute(() -> callback.onError(new ConflictException("Lịch đăng ký này đang có ca làm việc thực tế được phân công và không thể thay đổi.")));
                } else {
                    exec.mainThread().execute(() -> callback.onError(new Exception("Lỗi khi đăng ký lịch rảnh: " + response.code())));
                }
            } catch (Exception e) {
                exec.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void removeAvailability(int id, RepositoryCallback<Void> callback) {
        exec.diskIO().execute(() -> {
            try {
                Response<Void> response = apiService.removeAvailability(id).execute();
                if (response.isSuccessful()) {
                    exec.mainThread().execute(() -> callback.onSuccess(null));
                } else if (response.code() == 409) {
                    exec.mainThread().execute(() -> callback.onError(new ConflictException("Lịch đăng ký này đang có ca làm việc thực tế được phân công và không thể xóa.")));
                } else {
                    exec.mainThread().execute(() -> callback.onError(new Exception("Lỗi khi xóa lịch rảnh: " + response.code())));
                }
            } catch (Exception e) {
                exec.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void getActiveTemplates(RepositoryCallback<List<ShiftTemplateResponse>> callback) {
        exec.diskIO().execute(() -> {
            try {
                Response<List<ShiftTemplateResponse>> response = apiService.getActiveTemplates().execute();
                if (response.isSuccessful() && response.body() != null) {
                    exec.mainThread().execute(() -> callback.onSuccess(response.body()));
                } else {
                    exec.mainThread().execute(() -> callback.onError(new Exception("Lỗi khi tải ca mẫu đang hoạt động: " + response.code())));
                }
            } catch (Exception e) {
                exec.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void getAvailableStaffForShift(int templateId, int dayOfWeek, long shiftDate,
                                          RepositoryCallback<List<UserResponse>> callback) {
        exec.diskIO().execute(() -> {
            try {
                Response<List<UserResponse>> response = apiService.getAvailableStaffForShift(templateId, dayOfWeek, shiftDate).execute();
                if (response.isSuccessful() && response.body() != null) {
                    exec.mainThread().execute(() -> callback.onSuccess(response.body()));
                } else {
                    exec.mainThread().execute(() -> callback.onError(new Exception("Lỗi khi tải nhân viên khả dụng: " + response.code())));
                }
            } catch (Exception e) {
                exec.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void publishAvailability(PublishAvailabilityRequest request,
                                     RepositoryCallback<List<AvailabilityResponse>> callback) {
        exec.diskIO().execute(() -> {
            try {
                Response<List<AvailabilityResponse>> response =
                        apiService.publishAvailability(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    exec.mainThread().execute(() -> callback.onSuccess(response.body()));
                } else {
                    exec.mainThread().execute(() -> callback.onError(
                        new Exception("Lỗi khi phát hành lịch rảnh: " + response.code())));
                }
            } catch (Exception e) {
                exec.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void getWeekLock(long weekStart, RepositoryCallback<WeekLockResponse> callback) {
        exec.diskIO().execute(() -> {
            try {
                Response<WeekLockResponse> response =
                        apiService.getWeekLock(weekStart).execute();
                if (response.isSuccessful() && response.body() != null) {
                    exec.mainThread().execute(() -> callback.onSuccess(response.body()));
                } else {
                    exec.mainThread().execute(() -> callback.onError(
                        new Exception("Lỗi khi kiểm tra khóa tuần: " + response.code())));
                }
            } catch (Exception e) {
                exec.mainThread().execute(() -> callback.onError(e));
            }
        });
    }
}
