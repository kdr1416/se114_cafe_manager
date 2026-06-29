package com.example.cafe_manager.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.remote.RevenueReportResponse;
import com.example.cafe_manager.data.repository.RevenueRepository;
import com.example.cafe_manager.util.RepositoryCallback;

import java.util.Calendar;

public class RevenueReportViewModel extends AndroidViewModel {

    private final RevenueRepository repository;
    private int currentYear;
    private int currentMonth;
    private boolean isYearlyMode = false;

    private final MutableLiveData<RevenueReportResponse> reportData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public RevenueReportViewModel(@NonNull Application application) {
        super(application);
        this.repository = RevenueRepository.getInstance(application);

        Calendar calendar = Calendar.getInstance();
        this.currentYear = calendar.get(Calendar.YEAR);
        this.currentMonth = calendar.get(Calendar.MONTH) + 1;
    }

    public void loadCurrentMonth() {
        Calendar calendar = Calendar.getInstance();
        this.currentYear = calendar.get(Calendar.YEAR);
        this.currentMonth = calendar.get(Calendar.MONTH) + 1;
        this.isYearlyMode = false;
        loadMonth(currentYear, currentMonth);
    }

    public void loadMonth(int year, int month) {
        this.currentYear = year;
        this.currentMonth = month;
        this.isYearlyMode = false;
        this.isLoading.setValue(true);

        repository.getMonthlyRevenue(year, month, new RepositoryCallback<RevenueReportResponse>() {
            @Override
            public void onSuccess(RevenueReportResponse result) {
                isLoading.setValue(false);
                reportData.setValue(result);
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                errorMessage.setValue(e.getMessage());
            }
        });
    }

    public void loadYear(int year) {
        this.currentYear = year;
        this.isYearlyMode = true;
        this.isLoading.setValue(true);

        repository.getYearlySummary(year, new RepositoryCallback<RevenueReportResponse>() {
            @Override
            public void onSuccess(RevenueReportResponse result) {
                isLoading.setValue(false);
                reportData.setValue(result);
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                errorMessage.setValue(e.getMessage());
            }
        });
    }

    public void prevPeriod() {
        if (isYearlyMode) {
            currentYear--;
            loadYear(currentYear);
        } else {
            if (currentMonth == 1) {
                currentMonth = 12;
                currentYear--;
            } else {
                currentMonth--;
            }
            loadMonth(currentYear, currentMonth);
        }
    }

    public void nextPeriod() {
        if (!canGoNext()) {
            return;
        }
        if (isYearlyMode) {
            currentYear++;
            loadYear(currentYear);
        } else {
            if (currentMonth == 12) {
                currentMonth = 1;
                currentYear++;
            } else {
                currentMonth++;
            }
            loadMonth(currentYear, currentMonth);
        }
    }

    public boolean canGoNext() {
        Calendar calendar = Calendar.getInstance();
        int sysYear = calendar.get(Calendar.YEAR);
        int sysMonth = calendar.get(Calendar.MONTH) + 1;

        if (isYearlyMode) {
            return currentYear < sysYear;
        } else {
            return (currentYear < sysYear) || (currentYear == sysYear && currentMonth < sysMonth);
        }
    }

    public String getCurrentPeriodLabel() {
        if (isYearlyMode) {
            return "Năm " + currentYear;
        } else {
            return "Tháng " + currentMonth + "/" + currentYear;
        }
    }

    public void toggleMode() {
        if (isYearlyMode) {
            isYearlyMode = false;
            Calendar calendar = Calendar.getInstance();
            if (currentYear == calendar.get(Calendar.YEAR)) {
                currentMonth = calendar.get(Calendar.MONTH) + 1;
            } else {
                currentMonth = 1; // Default to Jan if switching from another year
            }
            loadMonth(currentYear, currentMonth);
        } else {
            isYearlyMode = true;
            loadYear(currentYear);
        }
    }

    // Getters for views
    public LiveData<RevenueReportResponse> getReportData() { return reportData; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public void clearErrorMessage() { errorMessage.setValue(null); }

    public int getCurrentYear() { return currentYear; }
    public int getCurrentMonth() { return currentMonth; }
    public boolean isYearlyMode() { return isYearlyMode; }
}
