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
import com.example.cafe_manager.util.RepositoryCallback;

public class ShiftReportViewModel extends AndroidViewModel {

    private final ShiftReportRepository reportRepository;
    private int shiftId = -1;

    private final MutableLiveData<ShiftEntity> shiftLiveData = new MutableLiveData<>();
    private final MutableLiveData<ShiftReportRepository.ShiftRevenueSummary> summaryLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    public ShiftReportViewModel(@NonNull Application application) {
        super(application);
        this.reportRepository = ShiftReportRepository.getInstance(application);
    }

    public void loadReport(int shiftId) {
        this.shiftId = shiftId;

        // Load shift info from network repository
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

        // Load revenue summary
        reportRepository.getShiftRevenueSummary(shiftId,
                new RepositoryCallback<ShiftReportRepository.ShiftRevenueSummary>() {
                    @Override
                    public void onSuccess(ShiftReportRepository.ShiftRevenueSummary result) {
                        summaryLiveData.setValue(result);
                    }

                    @Override
                    public void onError(Exception e) {
                        errorLiveData.setValue(e.getMessage());
                    }
                });
    }

    // ── LiveData getters ──

    public LiveData<ShiftEntity> getShift() { return shiftLiveData; }
    public LiveData<ShiftReportRepository.ShiftRevenueSummary> getSummary() { return summaryLiveData; }
    public LiveData<ShiftCashSessionEntity> getCashSession() {
        return reportRepository.getCashSessionLive(shiftId);
    }
    public LiveData<String> getError() { return errorLiveData; }

    public void clearError() { errorLiveData.setValue(null); }
}
