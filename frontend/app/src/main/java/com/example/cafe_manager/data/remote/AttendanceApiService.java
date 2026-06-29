package com.example.cafe_manager.data.remote;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import java.util.List;

public interface AttendanceApiService {
    @GET("api/v1/attendances")
    Call<List<AttendanceResponse>> getAllAttendances();

    @GET("api/v1/attendances/{id}")
    Call<AttendanceResponse> getAttendanceById(@Path("id") int id);

    @POST("api/v1/attendances/checkin")
    Call<AttendanceResponse> checkIn(@Body CheckInRequest request);

    @POST("api/v1/attendances/checkout")
    Call<AttendanceResponse> checkOut(@Body CheckOutRequest request);

    @GET("api/v1/attendances/shift/{shiftId}")
    Call<List<AttendanceResponse>> getAttendancesForShift(@Path("shiftId") int shiftId);

    @GET("api/v1/attendances/report/team")
    Call<List<TeamAttendanceSummary>> getTeamReport(@retrofit2.http.Query("year") int year, @retrofit2.http.Query("month") int month);

    @GET("api/v1/attendances/report/details")
    Call<UserAttendanceDetailResponse> getUserDetails(@retrofit2.http.Query("userId") int userId, @retrofit2.http.Query("year") int year, @retrofit2.http.Query("month") int month);
}
