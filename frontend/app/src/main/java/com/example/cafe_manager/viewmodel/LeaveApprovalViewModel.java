package com.example.cafe_manager.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.remote.ApiException;
import com.example.cafe_manager.data.remote.LeaveRequestResponse;
import com.example.cafe_manager.data.repository.LeaveRequestRepository;
import com.example.cafe_manager.util.RepositoryCallback;

import java.util.List;

public class LeaveApprovalViewModel extends AndroidViewModel {

    private final LeaveRequestRepository repository;

    private final MutableLiveData<List<LeaveRequestResponse>> requests = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> reviewing = new MutableLiveData<>(false);
    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> selectedStatus = new MutableLiveData<>("PENDING");

    public LeaveApprovalViewModel(@NonNull Application application) {
        super(application);
        this.repository = LeaveRequestRepository.getInstance(application);
    }

    public LiveData<List<LeaveRequestResponse>> getRequests() {
        return requests;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getReviewing() {
        return reviewing;
    }

    public LiveData<String> getMessage() {
        return message;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<String> getSelectedStatus() {
        return selectedStatus;
    }

    public void setSelectedStatus(String status) {
        selectedStatus.setValue(status);
    }

    public void clearMessage() {
        message.setValue(null);
    }

    public void clearError() {
        error.setValue(null);
    }

    public void loadRequests(String status) {
        loading.setValue(true);
        repository.getLeaveRequests(status, new RepositoryCallback<List<LeaveRequestResponse>>() {
            @Override
            public void onSuccess(List<LeaveRequestResponse> result) {
                loading.setValue(false);
                requests.setValue(result);
            }

            @Override
            public void onError(Exception e) {
                loading.setValue(false);
                if (e instanceof ApiException) {
                    int code = ((ApiException) e).getHttpCode();
                    if (code == -1) {
                        error.setValue("Không thể kết nối máy chủ");
                    } else {
                        error.setValue("Lỗi tải đơn xin nghỉ: " + e.getMessage());
                    }
                } else {
                    error.setValue("Không thể tải danh sách đơn xin nghỉ.");
                }
            }
        });
    }

    public void approveRequest(Long leaveRequestId, String reviewNote) {
        if (reviewing.getValue() != null && reviewing.getValue()) {
            return;
        }
        reviewing.setValue(true);
        repository.approveLeaveRequest(leaveRequestId, reviewNote, new RepositoryCallback<LeaveRequestResponse>() {
            @Override
            public void onSuccess(LeaveRequestResponse result) {
                reviewing.setValue(false);
                message.setValue("Đã duyệt đơn xin nghỉ");
                // Reload current list
                loadRequests(selectedStatus.getValue());
            }

            @Override
            public void onError(Exception e) {
                reviewing.setValue(false);
                mapReviewError(e, "duyệt");
            }
        });
    }

    public void rejectRequest(Long leaveRequestId, String reviewNote) {
        if (reviewing.getValue() != null && reviewing.getValue()) {
            return;
        }
        reviewing.setValue(true);
        repository.rejectLeaveRequest(leaveRequestId, reviewNote, new RepositoryCallback<LeaveRequestResponse>() {
            @Override
            public void onSuccess(LeaveRequestResponse result) {
                reviewing.setValue(false);
                message.setValue("Đã từ chối đơn xin nghỉ");
                // Reload current list
                loadRequests(selectedStatus.getValue());
            }

            @Override
            public void onError(Exception e) {
                reviewing.setValue(false);
                mapReviewError(e, "từ chối");
            }
        });
    }

    private void mapReviewError(Exception e, String action) {
        if (e instanceof ApiException) {
            int code = ((ApiException) e).getHttpCode();
            switch (code) {
                case 400:
                    error.setValue("Yêu cầu không hợp lệ (400)");
                    break;
                case 401:
                case 403:
                    error.setValue("Bạn không có quyền thực hiện thao tác này (403)");
                    break;
                case 404:
                    error.setValue("Không tìm thấy đơn xin nghỉ (404)");
                    break;
                case 409:
                    error.setValue("Đơn này đã được xử lý hoặc không còn ở trạng thái chờ duyệt (409)");
                    break;
                case -1:
                    error.setValue("Không thể kết nối máy chủ");
                    break;
                default:
                    error.setValue("Không thể " + action + " đơn xin nghỉ (Mã lỗi: " + code + ")");
                    break;
            }
        } else {
            error.setValue("Lỗi kết nối mạng hoặc lỗi không xác định.");
        }
    }
}
