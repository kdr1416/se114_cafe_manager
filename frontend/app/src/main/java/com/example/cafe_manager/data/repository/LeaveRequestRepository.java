package com.example.cafe_manager.data.repository;

import android.content.Context;
import com.example.cafe_manager.data.remote.ApiClient;
import com.example.cafe_manager.data.remote.ApiException;
import com.example.cafe_manager.data.remote.LeaveRequestApiService;
import com.example.cafe_manager.data.remote.LeaveRequestCreateRequest;
import com.example.cafe_manager.data.remote.LeaveRequestResponse;
import com.example.cafe_manager.data.remote.LeaveReviewRequest;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.RepositoryCallback;
import java.util.List;
import retrofit2.Response;

public class LeaveRequestRepository {
    private static volatile LeaveRequestRepository instance;
    private final LeaveRequestApiService apiService;
    private final AppExecutors exec;

    private LeaveRequestRepository(Context ctx) {
        this.apiService = ApiClient.getInstance(ctx).getService(LeaveRequestApiService.class);
        this.exec = AppExecutors.getInstance();
    }

    public static LeaveRequestRepository getInstance(Context ctx) {
        if (instance == null) {
            synchronized (LeaveRequestRepository.class) {
                if (instance == null) {
                    instance = new LeaveRequestRepository(ctx.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public void submitLeaveRequest(LeaveRequestCreateRequest request, RepositoryCallback<LeaveRequestResponse> callback) {
        exec.diskIO().execute(() -> {
            try {
                Response<LeaveRequestResponse> response = apiService.submitLeaveRequest(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    exec.mainThread().execute(() -> callback.onSuccess(response.body()));
                } else {
                    Exception err = handleHttpError(response, "Không thể gửi đơn xin nghỉ");
                    exec.mainThread().execute(() -> callback.onError(err));
                }
            } catch (Exception e) {
                exec.mainThread().execute(() -> callback.onError(new ApiException(-1, "Lỗi kết nối mạng: " + e.getMessage())));
            }
        });
    }

    public void getMyLeaveRequests(RepositoryCallback<List<LeaveRequestResponse>> callback) {
        exec.diskIO().execute(() -> {
            try {
                Response<List<LeaveRequestResponse>> response = apiService.getMyLeaveRequests().execute();
                if (response.isSuccessful() && response.body() != null) {
                    exec.mainThread().execute(() -> callback.onSuccess(response.body()));
                } else {
                    Exception err = handleHttpError(response, "Không thể tải danh sách đơn xin nghỉ");
                    exec.mainThread().execute(() -> callback.onError(err));
                }
            } catch (Exception e) {
                exec.mainThread().execute(() -> callback.onError(new ApiException(-1, "Lỗi kết nối mạng: " + e.getMessage())));
            }
        });
    }

    public void getLeaveRequests(String status, RepositoryCallback<List<LeaveRequestResponse>> callback) {
        exec.diskIO().execute(() -> {
            try {
                Response<List<LeaveRequestResponse>> response = apiService.getLeaveRequests(status).execute();
                if (response.isSuccessful() && response.body() != null) {
                    exec.mainThread().execute(() -> callback.onSuccess(response.body()));
                } else {
                    Exception err = handleHttpError(response, "Không thể tải danh sách đơn xin nghỉ quản lý");
                    exec.mainThread().execute(() -> callback.onError(err));
                }
            } catch (Exception e) {
                exec.mainThread().execute(() -> callback.onError(new ApiException(-1, "Lỗi kết nối mạng: " + e.getMessage())));
            }
        });
    }

    public void approveLeaveRequest(long leaveRequestId, String reviewNote, RepositoryCallback<LeaveRequestResponse> callback) {
        exec.diskIO().execute(() -> {
            try {
                LeaveReviewRequest request = new LeaveReviewRequest(reviewNote);
                Response<LeaveRequestResponse> response = apiService.approveLeaveRequest(leaveRequestId, request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    exec.mainThread().execute(() -> callback.onSuccess(response.body()));
                } else {
                    Exception err = handleHttpError(response, "Không thể duyệt đơn xin nghỉ");
                    exec.mainThread().execute(() -> callback.onError(err));
                }
            } catch (Exception e) {
                exec.mainThread().execute(() -> callback.onError(new ApiException(-1, "Lỗi kết nối mạng: " + e.getMessage())));
            }
        });
    }

    public void rejectLeaveRequest(long leaveRequestId, String reviewNote, RepositoryCallback<LeaveRequestResponse> callback) {
        exec.diskIO().execute(() -> {
            try {
                LeaveReviewRequest request = new LeaveReviewRequest(reviewNote);
                Response<LeaveRequestResponse> response = apiService.rejectLeaveRequest(leaveRequestId, request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    exec.mainThread().execute(() -> callback.onSuccess(response.body()));
                } else {
                    Exception err = handleHttpError(response, "Không thể từ chối đơn xin nghỉ");
                    exec.mainThread().execute(() -> callback.onError(err));
                }
            } catch (Exception e) {
                exec.mainThread().execute(() -> callback.onError(new ApiException(-1, "Lỗi kết nối mạng: " + e.getMessage())));
            }
        });
    }

    private Exception handleHttpError(Response<?> response, String defaultMessage) {
        if (response == null) {
            return new ApiException(-1, "Không có kết nối mạng");
        }
        int code = response.code();
        String message = defaultMessage + " (Mã lỗi: " + code + ")";
        try {
            if (response.errorBody() != null) {
                String errorBodyStr = response.errorBody().string();
                if (errorBodyStr != null && !errorBodyStr.trim().isEmpty()) {
                    message = errorBodyStr.trim();
                }
            }
        } catch (Exception ignored) {}

        switch (code) {
            case 400:
                return new ApiException(400, message.contains("Mã lỗi") ? "Yêu cầu không hợp lệ (400)" : message);
            case 401:
                return new ApiException(401, "Phiên làm việc hết hạn hoặc chưa đăng nhập.");
            case 403:
                return new ApiException(403, "Bạn không có quyền thực hiện hành động này (403).");
            case 404:
                return new ApiException(404, message.contains("Mã lỗi") ? "Không tìm thấy dữ liệu yêu cầu (404)" : message);
            case 409:
                return new ApiException(409, message.contains("Mã lỗi") ? "Có sự xung đột dữ liệu xảy ra (409)" : message);
            case 500:
                return new ApiException(500, "Lỗi máy chủ nội bộ. Vui lòng thử lại sau.");
            default:
                return new ApiException(code, message);
        }
    }
}
