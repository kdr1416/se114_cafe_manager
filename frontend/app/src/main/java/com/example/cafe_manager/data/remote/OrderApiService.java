package com.example.cafe_manager.data.remote;

import com.example.cafe_manager.data.local.entity.OrderEntity;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit interface kết nối đến OrderController backend.
 */
public interface OrderApiService {
    @POST("api/v1/orders")
    Call<OrderEntity> createOrder(@Body OrderRequest request);

    @GET("api/v1/orders")
    Call<List<OrderEntity>> getAllOrders(@Query("status") String status);

    @GET("api/v1/orders/{id}")
    Call<OrderDetailResponse> getOrderDetail(@Path("id") int orderId);

    @POST("api/v1/orders/{id}/items")
    Call<OrderDetailResponse> addItem(
            @Path("id") int orderId,
            @Body OrderItemRequest request
    );

    @POST("api/v1/orders/{id}/items/bulk")
    Call<OrderDetailResponse> addItemsBulk(
            @Path("id") int orderId,
            @Body List<OrderItemRequest> requests
    );

    @PUT("api/v1/orders/{id}/items/{itemId}")
    Call<OrderDetailResponse> updateItem(
            @Path("id") int orderId,
            @Path("itemId") int itemId,
            @Body Map<String, Integer> body
    );

    @DELETE("api/v1/orders/{id}/items/{itemId}")
    Call<OrderDetailResponse> removeItem(
            @Path("id") int orderId,
            @Path("itemId") int itemId
    );

    @PUT("api/v1/orders/{id}/confirm")
    Call<OrderEntity> confirmOrder(@Path("id") int orderId);

    @PUT("api/v1/orders/{id}/serve")
    Call<OrderEntity> serveOrder(@Path("id") int orderId);

    @PUT("api/v1/orders/{id}/cancel")
    Call<OrderEntity> cancelOrder(@Path("id") int orderId);

    @GET("api/v1/orders/history")
    Call<List<OrderDetailResponse>> getPaidOrdersHistory(
            @Query("from") long fromMs,
            @Query("to") long toMs
    );
}
