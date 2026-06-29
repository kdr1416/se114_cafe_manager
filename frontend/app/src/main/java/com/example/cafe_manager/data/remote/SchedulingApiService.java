package com.example.cafe_manager.data.remote;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface SchedulingApiService {

    @POST("api/v1/scheduling/preview")
    Call<SchedulingResponse> runPreview(@Body SchedulingRequest request);

    @GET("api/v1/scheduling/preview/{runId}")
    Call<SchedulingResponse> getPreview(@Path("runId") long runId);

    @POST("api/v1/scheduling/preview/{runId}/apply")
    Call<SchedulingResponse> applyPreview(@Path("runId") long runId);
}
