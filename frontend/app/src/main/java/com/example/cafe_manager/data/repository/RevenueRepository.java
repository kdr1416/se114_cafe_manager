package com.example.cafe_manager.data.repository;

import android.content.Context;
import com.example.cafe_manager.data.remote.ApiClient;
import com.example.cafe_manager.data.remote.RevenueApiService;
import com.example.cafe_manager.data.remote.RevenueReportResponse;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.RepositoryCallback;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RevenueRepository {
    private static volatile RevenueRepository instance;
    private final RevenueApiService apiService;
    private final AppExecutors appExecutors;

    private RevenueRepository(Context context) {
        this.apiService = ApiClient.getInstance(context).getService(RevenueApiService.class);
        this.appExecutors = AppExecutors.getInstance();
    }

    public static RevenueRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (RevenueRepository.class) {
                if (instance == null) {
                    instance = new RevenueRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public void getMonthlyRevenue(int year, int month, RepositoryCallback<RevenueReportResponse> callback) {
        appExecutors.diskIO().execute(() -> {
            apiService.getMonthlyRevenue(year, month).enqueue(new Callback<RevenueReportResponse>() {
                @Override
                public void onResponse(Call<RevenueReportResponse> call, Response<RevenueReportResponse> response) {
                    appExecutors.mainThread().execute(() -> {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            String errorMsg = "Lỗi khi lấy báo cáo doanh thu theo tháng: " + response.code();
                            try {
                                if (response.errorBody() != null) {
                                    errorMsg = response.errorBody().string();
                                }
                            } catch (Exception ignored) {}
                            callback.onError(new Exception(errorMsg));
                        }
                    });
                }

                @Override
                public void onFailure(Call<RevenueReportResponse> call, Throwable t) {
                    appExecutors.mainThread().execute(() -> {
                        callback.onError(new Exception("Lỗi kết nối: " + t.getMessage(), t));
                    });
                }
            });
        });
    }

    public void getYearlySummary(int year, RepositoryCallback<RevenueReportResponse> callback) {
        appExecutors.diskIO().execute(() -> {
            apiService.getYearlySummary(year).enqueue(new Callback<RevenueReportResponse>() {
                @Override
                public void onResponse(Call<RevenueReportResponse> call, Response<RevenueReportResponse> response) {
                    appExecutors.mainThread().execute(() -> {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            String errorMsg = "Lỗi khi lấy báo cáo doanh thu cả năm: " + response.code();
                            try {
                                if (response.errorBody() != null) {
                                    errorMsg = response.errorBody().string();
                                }
                            } catch (Exception ignored) {}
                            callback.onError(new Exception(errorMsg));
                        }
                    });
                }

                @Override
                public void onFailure(Call<RevenueReportResponse> call, Throwable t) {
                    appExecutors.mainThread().execute(() -> {
                        callback.onError(new Exception("Lỗi kết nối: " + t.getMessage(), t));
                    });
                }
            });
        });
    }
}
