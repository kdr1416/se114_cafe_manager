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
import com.example.cafe_manager.manager.SessionManager;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.RepositoryCallback;

public class ShiftCloseViewModel extends AndroidViewModel {

    private final ShiftReportRepository reportRepository;
    private final AppDatabase appDatabase;
    private final SessionManager sessionManager;

    private int shiftId = -1;

    private final MutableLiveData<ShiftEntity> shiftLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> unpaidCountLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> closeSuccessLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    public ShiftCloseViewModel(@NonNull Application application) {
        super(application);
        this.reportRepository = new ShiftReportRepository(application);
        this.appDatabase = AppDatabase.getInstance(application);
        this.sessionManager = SessionManager.getInstance(application);
    }

    public void setShiftId(int shiftId) {
        this.shiftId = shiftId;
        loadShiftInfo();
        loadUnpaidCount();
    }

    private void loadShiftInfo() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            ShiftEntity shift = appDatabase.shiftDao().getById(shiftId);
            AppExecutors.getInstance().mainThread().execute(() -> shiftLiveData.setValue(shift));
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
