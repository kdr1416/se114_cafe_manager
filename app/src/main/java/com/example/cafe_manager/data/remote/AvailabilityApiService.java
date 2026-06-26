package com.example.cafe_manager.data.remote;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import java.util.List;

public interface AvailabilityApiService {

    @GET("api/v1/availability/me")
    Call<List<AvailabilityResponse>> getMyAvailability();

    @POST("api/v1/availability")
    Call<AvailabilityResponse> setAvailability(@Body SetAvailabilityRequest request);

    @DELETE("api/v1/availability/{id}")
    Call<Void> removeAvailability(@Path("id") int availabilityId);

    @GET("api/v1/availability/shift-templates/active")
    Call<List<ShiftTemplateResponse>> getActiveTemplates();

    @GET("api/v1/availability/staff/available")
    Call<List<UserResponse>> getAvailableStaffForShift(
        @Query("templateId") int templateId,
        @Query("dayOfWeek") int dayOfWeek,
        @Query("shiftDate") long shiftDate
    );

    @POST("api/v1/availability/publish")
    Call<List<AvailabilityResponse>> publishAvailability(@Body PublishAvailabilityRequest request);

    @GET("api/v1/availability/week-lock")
    Call<WeekLockResponse> getWeekLock(@Query("weekStart") long weekStart);
}
