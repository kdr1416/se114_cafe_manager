package com.example.cafe_manager.data.remote;

import com.example.cafe_manager.data.local.entity.PromotionEntity;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit interface kết nối đến PromotionController backend.
 */
public interface PromotionApiService {
    @GET("api/v1/promotions")
    Call<List<PromotionEntity>> getAll();

    @GET("api/v1/promotions/count")
    Call<Long> getTotalCount();

    @GET("api/v1/promotions/active-count")
    Call<Long> getActiveCount();

    @GET("api/v1/promotions/code/{code}")
    Call<PromotionEntity> getByCode(@Path("code") String code);

    @POST("api/v1/promotions")
    Call<PromotionEntity> insert(@Body PromotionEntity promotion);

    @PUT("api/v1/promotions/{id}")
    Call<PromotionEntity> update(@Path("id") int id, @Body PromotionEntity promotion);

    @PUT("api/v1/promotions/{id}/toggle-active")
    Call<Void> toggleActive(@Path("id") int id, @Query("active") boolean active);

    @DELETE("api/v1/promotions/{id}")
    Call<Void> delete(@Path("id") int id);
}
