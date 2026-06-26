package com.example.cafe_manager.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.remote.ApiException;
import com.example.cafe_manager.data.remote.LeaveRequestCreateRequest;
import com.example.cafe_manager.data.remote.LeaveRequestResponse;
import com.example.cafe_manager.data.repository.LeaveRequestRepository;
import com.example.cafe_manager.util.RepositoryCallback;

import java.util.List;

public class LeaveRequestViewModel extends AndroidViewModel {

    private final LeaveRequestRepository repository;

    private final MutableLiveData<List<LeaveRequestResponse>> myRequests = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> submitting = new MutableLiveData<>(false);
    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public LeaveRequestViewModel(@NonNull Application application) {
        super(application);
        this.repository = LeaveRequestRepository.getInstance(application);
    }

    public LiveData<List<LeaveRequestResponse>> getMyRequests() {
        return myRequests;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getSubmitting() {
        return submitting;
    }

    public LiveData<String> getMessage() {
        return message;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void clearMessage() {
        message.setValue(null);
    }

    public void clearError() {
        error.setValue(null);
    }

    public void loadMyRequests() {
        loading.setValue(true);
        repository.getMyLeaveRequests(new RepositoryCallback<List<LeaveRequestResponse>>() {
            @Override
            public void onSuccess(List<LeaveRequestResponse> result) {
                loading.setValue(false);
                myRequests.setValue(result);
            }

            @Override
            public void onError(Exception e) {
                loading.setValue(false);
                if (e instanceof ApiException) {
                    int code = ((ApiException) e).getHttpCode();
                    if (code == -1) {
                        error.setValue("Không thể kết nối máy chủ");
                    } else {
                        error.setValue("Lỗi khi tải đơn xin nghỉ: " + e.getMessage());
                    }
                } else {
                    error.setValue("Không thể tải đơn xin nghỉ cá nhân.");
                }
            }
        });
    }

    public void submitLeaveRequest(Long startAt, Long endAt, String reason) {
        if (submitting.getValue() != null && submitting.getValue()) {
            return; // Ngăn gửi lặp
        }

        if (startAt == null) {
            error.setValue("Vui lòng chọn thời gian bắt đầu");
            return;
        }
        if (endAt == null) {
            error.setValue("Vui lòng chọn thời gian kết thúc");
            return;
        }
        if (startAt >= endAt) {
            error.setValue("Thời gian bắt đầu phải trước thời gian kết thúc");
            return;
        }
        if (reason == null || reason.trim().isEmpty()) {
            error.setValue("Vui lòng nhập lý do xin nghỉ");
            return;
        }

        submitting.setValue(true);
        LeaveRequestCreateRequest request = new LeaveRequestCreateRequest(startAt, endAt, reason.trim());

        repository.submitLeaveRequest(request, new RepositoryCallback<LeaveRequestResponse>() {
            @Override
            public void onSuccess(LeaveRequestResponse result) {
                submitting.setValue(false);
                message.setValue("Gửi đơn xin nghỉ thành công");
                loadMyRequests(); // Reload list
            }

            @Override
            public void onError(Exception e) {
                submitting.setValue(false);
                if (e instanceof ApiException) {
                    int code = ((ApiException) e).getHttpCode();
                    switch (code) {
                        case 400:
                            error.setValue("Thông tin đơn xin nghỉ không hợp lệ");
                            break;
                        case 401:
                        case 403:
                            error.setValue("Bạn không có quyền thực hiện thao tác này");
                            break;
                        case 409:
                            error.setValue("Bạn đã có đơn xin nghỉ trùng thời gian đang chờ duyệt hoặc đã được duyệt");
                            break;
                        case -1:
                            error.setValue("Không thể kết nối máy chủ");
                            break;
                        default:
                            error.setValue("Gửi đơn xin nghỉ thất bại (Mã lỗi: " + code + ")");
                            break;
                    }
                } else {
                    error.setValue("Gửi đơn xin nghỉ thất bại");
                }
            }
        });
    }
}
