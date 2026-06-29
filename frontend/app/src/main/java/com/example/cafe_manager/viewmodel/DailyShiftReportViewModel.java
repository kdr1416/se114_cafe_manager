package com.example.cafe_manager.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.cafe_manager.data.remote.DailyShiftReportResponse;
import com.example.cafe_manager.data.repository.ShiftReportRepository;
import com.example.cafe_manager.util.RepositoryCallback;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DailyShiftReportViewModel extends AndroidViewModel {

    private final ShiftReportRepository reportRepository;
    private final MutableLiveData<LocalDate> selectedDate = new MutableLiveData<>();
    private final MutableLiveData<DailyShiftReportResponse> dailyReport = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public DailyShiftReportViewModel(@NonNull Application application) {
        super(application);
        this.reportRepository = ShiftReportRepository.getInstance(application);
        // Mặc định ngày hôm nay
        selectedDate.setValue(LocalDate.now());
    }

    public void selectDate(LocalDate date) {
        if (date != null) {
            selectedDate.setValue(date);
            loadDailyReport();
        }
    }

    public void nextDay() {
        LocalDate current = selectedDate.getValue();
        if (current != null) {
            selectedDate.setValue(current.plusDays(1));
            loadDailyReport();
        }
    }

    public void prevDay() {
        LocalDate current = selectedDate.getValue();
        if (current != null) {
            selectedDate.setValue(current.minusDays(1));
            loadDailyReport();
        }
    }

    public void loadDailyReport() {
        LocalDate date = selectedDate.getValue();
        if (date == null) return;

        loading.setValue(true);
        String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        reportRepository.getDailyShiftReport(dateStr, new RepositoryCallback<DailyShiftReportResponse>() {
            @Override
            public void onSuccess(DailyShiftReportResponse result) {
                loading.setValue(false);
                dailyReport.setValue(result);
            }

            @Override
            public void onError(Exception e) {
                loading.setValue(false);
                error.setValue(e.getMessage());
            }
        });
    }

    public LiveData<LocalDate> getSelectedDate() { return selectedDate; }
    public LiveData<DailyShiftReportResponse> getDailyReport() { return dailyReport; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }
    public void clearError() { error.setValue(null); }
}
