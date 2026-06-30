package com.example.cafe_manager.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.cafe_manager.data.remote.TeamAttendanceSummary;
import com.example.cafe_manager.data.remote.UserAttendanceDetailResponse;
import com.example.cafe_manager.data.repository.AttendanceReportRepository;
import com.example.cafe_manager.util.RepositoryCallback;
import java.util.Calendar;
import java.util.List;

public class AttendanceReportViewModel extends AndroidViewModel {

    private final AttendanceReportRepository repository;
    private final MutableLiveData<Integer> currentYear = new MutableLiveData<>();
    private final MutableLiveData<Integer> currentMonth = new MutableLiveData<>();
    private final MutableLiveData<List<TeamAttendanceSummary>> teamSummaries = new MutableLiveData<>();
    private final MutableLiveData<UserAttendanceDetailResponse> selectedUserDetail = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private Integer selectedUserId = null;

    public AttendanceReportViewModel(@NonNull Application application) {
        super(application);
        repository = AttendanceReportRepository.getInstance(application);
        Calendar cal = Calendar.getInstance();
        currentYear.setValue(cal.get(Calendar.YEAR));
        currentMonth.setValue(cal.get(Calendar.MONTH) + 1); // 1-indexed
    }

    public LiveData<Integer> getCurrentYear() { return currentYear; }
    public LiveData<Integer> getCurrentMonth() { return currentMonth; }
    public LiveData<List<TeamAttendanceSummary>> getTeamSummaries() { return teamSummaries; }
    public LiveData<UserAttendanceDetailResponse> getSelectedUserDetail() { return selectedUserDetail; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public void clearErrorMessage() { errorMessage.setValue(null); }

    public Integer getSelectedUserId() {
        return selectedUserId;
    }

    public void setSelectedUserId(Integer selectedUserId) {
        this.selectedUserId = selectedUserId;
    }

    public void nextMonth() {
        Integer m = currentMonth.getValue();
        Integer y = currentYear.getValue();
        if (m != null && y != null) {
            if (m == 12) {
                currentMonth.setValue(1);
                currentYear.setValue(y + 1);
            } else {
                currentMonth.setValue(m + 1);
            }
            loadReport();
        }
    }

    public void prevMonth() {
        Integer m = currentMonth.getValue();
        Integer y = currentYear.getValue();
        if (m != null && y != null) {
            if (m == 1) {
                currentMonth.setValue(12);
                currentYear.setValue(y - 1);
            } else {
                currentMonth.setValue(m - 1);
            }
            loadReport();
        }
    }

    public void loadReport() {
        Integer m = currentMonth.getValue();
        Integer y = currentYear.getValue();
        if (m == null || y == null) return;

        isLoading.setValue(true);
        repository.getTeamReport(y, m, new RepositoryCallback<List<TeamAttendanceSummary>>() {
            @Override
            public void onSuccess(List<TeamAttendanceSummary> result) {
                isLoading.setValue(false);
                teamSummaries.setValue(result);
                if (selectedUserId != null) {
                    loadUserDetails(selectedUserId);
                } else if (result != null && !result.isEmpty()) {
                    loadUserDetails(result.get(0).getUserId());
                } else {
                    selectedUserDetail.setValue(null);
                }
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                errorMessage.setValue("Lỗi tải báo cáo: " + e.getMessage());
            }
        });
    }

    public void loadUserDetails(int userId) {
        Integer m = currentMonth.getValue();
        Integer y = currentYear.getValue();
        if (m == null || y == null) return;

        selectedUserId = userId;
        isLoading.setValue(true);
        repository.getUserDetails(userId, y, m, new RepositoryCallback<UserAttendanceDetailResponse>() {
            @Override
            public void onSuccess(UserAttendanceDetailResponse result) {
                isLoading.setValue(false);
                selectedUserDetail.setValue(result);
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                errorMessage.setValue("Lỗi tải chi tiết: " + e.getMessage());
            }
        });
    }

    public String getCurrentPeriodLabel() {
        Integer m = currentMonth.getValue();
        Integer y = currentYear.getValue();
        if (m != null && y != null) {
            return String.format("Tháng %02d / %04d", m, y);
        }
        return "";
    }
}
