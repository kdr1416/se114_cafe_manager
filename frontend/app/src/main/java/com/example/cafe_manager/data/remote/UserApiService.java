package com.example.cafe_manager.data.remote;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * Retrofit interface kết nối đến UserController backend.
 */
public interface UserApiService {
    @GET("api/v1/users")
    Call<List<UserResponse>> getAllUsers();

    @POST("api/v1/users")
    Call<UserResponse> createUser(@Body CreateUserRequest request);

    @GET("api/v1/users/{id}")
    Call<UserResponse> getUserDetail(@Path("id") int id);

    @PUT("api/v1/users/{id}")
    Call<UserResponse> updateUser(@Path("id") int id, @Body UpdateUserRequest request);

    @PUT("api/v1/users/{id}/password")
    Call<Void> changePassword(@Path("id") int id, @Body ChangePasswordRequest request);

    @PUT("api/v1/users/{id}/status")
    Call<Void> updateUserStatus(@Path("id") int id, @Body UpdateUserStatusRequest request);
}
