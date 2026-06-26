package com.example.cafe_manager.data.remote;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface LeaveRequestApiService {

    @POST("api/v1/leave-requests")
    Call<LeaveRequestResponse> submitLeaveRequest(@Body LeaveRequestCreateRequest request);

    @GET("api/v1/leave-requests/my")
    Call<List<LeaveRequestResponse>> getMyLeaveRequests();

    @GET("api/v1/leave-requests")
    Call<List<LeaveRequestResponse>> getLeaveRequests(@Query("status") String status);

    @PUT("api/v1/leave-requests/{id}/approve")
    Call<LeaveRequestResponse> approveLeaveRequest(
            @Path("id") long id,
            @Body LeaveReviewRequest request
    );

    @PUT("api/v1/leave-requests/{id}/reject")
    Call<LeaveRequestResponse> rejectLeaveRequest(
            @Path("id") long id,
            @Body LeaveReviewRequest request
    );
}
