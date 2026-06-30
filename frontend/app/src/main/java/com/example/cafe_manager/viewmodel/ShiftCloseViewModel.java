package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.local.entity.ShiftCashSessionEntity;
import com.example.cafe_manager.data.local.entity.ShiftEntity;
import com.example.cafe_manager.data.repository.ShiftReportRepository;
import com.example.cafe_manager.data.repository.ShiftRepository;
import com.example.cafe_manager.manager.SessionManager;
import com.example.cafe_manager.util.RepositoryCallback;

public class ShiftCloseViewModel extends AndroidViewModel {

    private final ShiftReportRepository reportRepository;
    private final SessionManager sessionManager;

    private int shiftId = -1;

    private final MutableLiveData<ShiftEntity> shiftLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> unpaidCountLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> closeSuccessLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    public ShiftCloseViewModel(@NonNull Application application) {
        super(application);
        this.reportRepository = ShiftReportRepository.getInstance(application);
        this.sessionManager = SessionManager.getInstance(application);
    }

    public void setShiftId(int shiftId) {
        this.shiftId = shiftId;
        loadShiftInfo();
        loadUnpaidCount();
    }

    private void loadShiftInfo() {
        ShiftRepository.getInstance(getApplication()).getShiftById(shiftId, new RepositoryCallback<ShiftEntity>() {
            @Override
            public void onSuccess(ShiftEntity result) {
                shiftLiveData.setValue(result);
            }

            @Override
            public void onError(Exception e) {
                errorLiveData.setValue("Lỗi tải thông tin ca: " + e.getMessage());
            }
        });
    }

    private void loadUnpaidCount() {
        reportRepository.getUnpaidOrderCount(shiftId, new RepositoryCallback<Integer>() {
            @Override
            public void onSuccess(Integer count) {
                unpaidCountLiveData.setValue(count);
            }

            @Override
            public void onError(Exception e) {
                unpaidCountLiveData.setValue(0);
            }
        });
    }

    public void closeShift(double actualCash) {
        if (shiftId == -1) {
            errorLiveData.setValue("Không xác định được ca.");
            return;
        }
        if (actualCash < 0) {
            errorLiveData.setValue("Số tiền thực tế không được âm.");
            return;
        }

        // Chặn đóng ca khi vẫn còn hóa đơn/order chưa thanh toán
        Integer unpaid = unpaidCountLiveData.getValue();
        if (unpaid == null) {
            errorLiveData.setValue("Đang tải dữ liệu kiểm tra đơn chưa thanh toán. Vui lòng thử lại sau.");
            return;
        }
        if (unpaid > 0) {
            errorLiveData.setValue("Không thể đóng ca khi còn " + unpaid + " đơn chưa thanh toán.");
            return;
        }

        loadingLiveData.setValue(true);
        int closedBy = sessionManager.getUserId();

        reportRepository.closeCashSession(shiftId, actualCash, closedBy,
                new RepositoryCallback<ShiftCashSessionEntity>() {
                    @Override
                    public void onSuccess(ShiftCashSessionEntity result) {
                        loadingLiveData.setValue(false);
                        closeSuccessLiveData.setValue(true);
                    }

                    @Override
                    public void onError(Exception e) {
                        loadingLiveData.setValue(false);
                        errorLiveData.setValue(e.getMessage());
                    }
                });
    }

    // ── LiveData getters ──

    public LiveData<ShiftEntity> getShift() { return shiftLiveData; }
    public LiveData<Integer> getUnpaidCount() { return unpaidCountLiveData; }
    public LiveData<Boolean> getLoading() { return loadingLiveData; }
    public LiveData<Boolean> getCloseSuccess() { return closeSuccessLiveData; }
    public LiveData<String> getError() { return errorLiveData; }
    public LiveData<ShiftCashSessionEntity> getCashSession() {
        return reportRepository.getCashSessionLive(shiftId);
    }

    public void clearError() { errorLiveData.setValue(null); }
    public void clearCloseSuccess() { closeSuccessLiveData.setValue(null); }
}
