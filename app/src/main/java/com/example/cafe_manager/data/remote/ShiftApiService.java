package com.example.cafe_manager.data.remote;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import java.util.List;

public interface ShiftApiService {

    // ── Shift Templates ──
    @GET("api/v1/shift-templates")
    Call<List<ShiftTemplateResponse>> getAllTemplates();

    @GET("api/v1/shift-templates/{id}")
    Call<ShiftTemplateResponse> getTemplateById(@Path("id") int id);

    @POST("api/v1/shift-templates")
    Call<ShiftTemplateResponse> createTemplate(@Body ShiftTemplateResponse template);

    @PUT("api/v1/shift-templates/{id}")
    Call<ShiftTemplateResponse> updateTemplate(@Path("id") int id, @Body ShiftTemplateResponse template);

    @PUT("api/v1/shift-templates/{id}/deactivate")
    Call<Void> deactivateTemplate(@Path("id") int id);

    // ── Shifts ──
    @GET("api/v1/shifts")
    Call<List<ShiftResponse>> getShifts(@Query("date") String date, @Query("status") String status);

    @GET("api/v1/shifts/{id}")
    Call<ShiftResponse> getShiftById(@Path("id") int id);

    @POST("api/v1/shifts")
    Call<ShiftResponse> createShift(@Body CreateShiftRequest request);

    @PUT("api/v1/shifts/{id}/publish")
    Call<ShiftResponse> publishShift(@Path("id") int id);

    @PUT("api/v1/shifts/{id}/open")
    Call<ShiftResponse> openShift(@Path("id") int id, @Body OpenShiftRequest request);

    @PUT("api/v1/shifts/{id}/close")
    Call<ShiftResponse> closeShift(@Path("id") int id, @Body CloseShiftRequest request);

    @PUT("api/v1/shifts/{id}/cancel")
    Call<ShiftResponse> cancelShift(@Path("id") int id);

    @POST("api/v1/shifts/{id}/assign")
    Call<Void> assignStaff(@Path("id") int id, @Body AssignStaffRequest request);

    @DELETE("api/v1/shifts/{id}/assign/{userId}")
    Call<Void> unassignStaff(@Path("id") int id, @Path("userId") int userId);

    @GET("api/v1/shifts/{id}/report")
    Call<ShiftReportResponse> getShiftReport(@Path("id") int id);

    @GET("api/v1/shifts/{id}/assignments")
    Call<List<ShiftAssignmentResponse>> getAssignments(@Path("id") int id);

    // ── Shift Assignments ──
    @GET("api/v1/assignments/{assignmentId}")
    Call<ShiftAssignmentResponse> getAssignment(@Path("assignmentId") int assignmentId);

    @PUT("api/v1/assignments/{assignmentId}/confirm")
    Call<Void> confirmAssignment(@Path("assignmentId") int assignmentId);
}
