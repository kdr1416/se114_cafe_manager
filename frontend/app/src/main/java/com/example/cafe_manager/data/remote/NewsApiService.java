package com.example.cafe_manager.data.remote;

import com.example.cafe_manager.data.local.entity.NewsPostEntity;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import java.util.List;

public interface NewsApiService {
    @GET("api/v1/news")
    Call<List<NewsPostResponse>> getAllPosts();

    @GET("api/v1/news/{id}")
    Call<NewsPostResponse> getPostById(@Path("id") int id);

    @POST("api/v1/news")
    Call<NewsPostResponse> createPost(@Body CreateNewsRequest request);

    @PUT("api/v1/news/{id}")
    Call<NewsPostResponse> updatePost(@Path("id") int id, @Body UpdateNewsRequest request);

    @DELETE("api/v1/news/{id}")
    Call<Void> deletePost(@Path("id") int id);

    @POST("api/v1/news/{id}/read")
    Call<Void> markRead(@Path("id") int id);

    @GET("api/v1/news/unread-count")
    Call<Integer> getUnreadCount();
}