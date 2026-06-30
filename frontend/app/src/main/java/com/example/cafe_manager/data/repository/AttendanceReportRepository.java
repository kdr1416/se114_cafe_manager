package com.example.cafe_manager.data.repository;

import android.content.Context;
import com.example.cafe_manager.data.remote.ApiClient;
import com.example.cafe_manager.data.remote.AttendanceApiService;
import com.example.cafe_manager.data.remote.TeamAttendanceSummary;
import com.example.cafe_manager.data.remote.UserAttendanceDetailResponse;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.RepositoryCallback;
import java.util.List;

public class AttendanceReportRepository {

    private static volatile AttendanceReportRepository instance;
    private final AttendanceApiService apiService;
    private final AppExecutors exec;

    private AttendanceReportRepository(Context ctx) {
        this.apiService = ApiClient.getInstance(ctx).getService(AttendanceApiService.class);
        this.exec = AppExecutors.getInstance();
    }

    public static AttendanceReportRepository getInstance(Context ctx) {
        if (instance == null) {
            synchronized (AttendanceReportRepository.class) {
                if (instance == null) {
                    instance = new AttendanceReportRepository(ctx.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public void getTeamReport(int year, int month, RepositoryCallback<List<TeamAttendanceSummary>> callback) {
        exec.diskIO().execute(() -> {
            try {
                retrofit2.Response<List<TeamAttendanceSummary>> response = apiService.getTeamReport(year, month).execute();
                exec.mainThread().execute(() -> {
                    if (response.isSuccessful() && response.body() != null) {
                        callback.onSuccess(response.body());
                    } else {
                        callback.onError(new Exception("Lỗi lấy báo cáo chấm công của đội."));
                    }
                });
            } catch (Exception e) {
                exec.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void getUserDetails(int userId, int year, int month, RepositoryCallback<UserAttendanceDetailResponse> callback) {
        exec.diskIO().execute(() -> {
            try {
                retrofit2.Response<UserAttendanceDetailResponse> response = apiService.getUserDetails(userId, year, month).execute();
                exec.mainThread().execute(() -> {
                    if (response.isSuccessful() && response.body() != null) {
                        callback.onSuccess(response.body());
                    } else {
                        callback.onError(new Exception("Lỗi lấy chi tiết chấm công nhân viên."));
                    }
                });
            } catch (Exception e) {
                exec.mainThread().execute(() -> callback.onError(e));
            }
        });
    }
}
