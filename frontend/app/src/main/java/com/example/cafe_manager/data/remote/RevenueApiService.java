package com.example.cafe_manager.data.remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface RevenueApiService {
    @GET("api/v1/reports/revenue")
    Call<RevenueReportResponse> getMonthlyRevenue(
            @Query("year") int year,
            @Query("month") int month
    );

    @GET("api/v1/reports/revenue/summary")
    Call<RevenueReportResponse> getYearlySummary(
            @Query("year") int year
    );
}
