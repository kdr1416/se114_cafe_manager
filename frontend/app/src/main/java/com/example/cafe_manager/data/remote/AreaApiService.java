package com.example.cafe_manager.data.remote;

import com.example.cafe_manager.data.local.entity.AreaEntity;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import java.util.List;

public interface AreaApiService {
    @GET("api/v1/areas")
    Call<List<AreaResponse>> getAllAreas();

    @GET("api/v1/areas/{id}")
    Call<AreaResponse> getAreaById(@Path("id") int id);

    @POST("api/v1/areas")
    Call<AreaResponse> createArea(@Body CreateAreaRequest request);

    @PUT("api/v1/areas/{id}")
    Call<AreaResponse> updateArea(@Path("id") int id, @Body UpdateAreaRequest request);

    @DELETE("api/v1/areas/{id}")
    Call<Void> deleteArea(@Path("id") int id);
}