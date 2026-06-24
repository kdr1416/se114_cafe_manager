package com.example.cafe_manager.data.remote;

import com.example.cafe_manager.data.local.entity.PaymentEntity;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit interface kết nối đến PaymentController backend.
 */
public interface PaymentApiService {
    @POST("api/v1/payments")
    Call<PaymentResponse> processPayment(@Body PaymentRequest request);

    @GET("api/v1/payments/{orderId}")
    Call<PaymentEntity> getPaymentByOrderId(@Path("orderId") int orderId);

    @GET("api/v1/payments/revenue")
    Call<Double> getRevenueInRange(
            @Query("startDate") long startDate,
            @Query("endDate") long endDate
    );

    @GET("api/v1/payments/count")
    Call<Long> countPaymentsInRange(
            @Query("startDate") long startDate,
            @Query("endDate") long endDate
    );
}
