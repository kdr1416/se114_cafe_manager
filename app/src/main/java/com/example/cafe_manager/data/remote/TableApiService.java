package com.example.cafe_manager.data.remote;

import com.example.cafe_manager.data.local.entity.TableEntity;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * Retrofit interface kết nối đến TableController backend.
 */
public interface TableApiService {
    @GET("api/v1/tables")
    Call<List<TableEntity>> getAllTables();

    @GET("api/v1/tables/{id}")
    Call<TableEntity> getTableById(@Path("id") int id);

    @POST("api/v1/tables")
    Call<TableEntity> createTable(@Body TableEntity table);

    @PUT("api/v1/tables/{id}")
    Call<TableEntity> updateTable(@Path("id") int id, @Body TableEntity table);

    @DELETE("api/v1/tables/{id}")
    Call<Void> deleteTable(@Path("id") int id);
}
