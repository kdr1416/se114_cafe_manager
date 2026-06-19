package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.entity.ShiftCashSessionEntity;
import com.example.cafe_manager.data.local.entity.ShiftEntity;
import com.example.cafe_manager.data.repository.ShiftReportRepository;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.RepositoryCallback;

public class ShiftReportViewModel extends AndroidViewModel {

    private final ShiftReportRepository reportRepository;
    private final AppDatabase appDatabase;

    private int shiftId = -1;

    private final MutableLiveData<ShiftEntity> shiftLiveData = new MutableLiveData<>();
    private final MutableLiveData<ShiftReportRepository.ShiftRevenueSummary> summaryLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    public ShiftReportViewModel(@NonNull Application application) {
        super(application);
        this.reportRepository = new ShiftReportRepository(application);
        this.appDatabase = AppDatabase.getInstance(application);
    }

    public void loadReport(int shiftId) {
        this.shiftId = shiftId;

        // Load shift info
        AppExecutors.getInstance().diskIO().execute(() -> {
            ShiftEntity shift = appDatabase.shiftDao().getById(shiftId);
            AppExecutors.getInstance().mainThread().execute(() -> shiftLiveData.setValue(shift));
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
