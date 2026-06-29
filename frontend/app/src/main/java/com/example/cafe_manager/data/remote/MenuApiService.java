package com.example.cafe_manager.data.remote;

import com.example.cafe_manager.data.local.entity.CategoryEntity;
import com.example.cafe_manager.data.local.entity.ProductEntity;

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
 * Retrofit interface kết nối đến CategoryController và ProductController backend.
 */
public interface MenuApiService {
    // === Category APIs ===
    @GET("api/v1/categories")
    Call<List<CategoryEntity>> getActiveCategories();

    @POST("api/v1/categories")
    Call<CategoryEntity> createCategory(@Body CategoryEntity category);

    @PUT("api/v1/categories/{id}")
    Call<CategoryEntity> updateCategory(@Path("id") int id, @Body CategoryEntity category);

    @DELETE("api/v1/categories/{id}")
    Call<Void> deleteCategory(@Path("id") int id);

    // === Product APIs ===
    @GET("api/v1/products")
    Call<List<ProductEntity>> getProducts(
            @Query("categoryId") Integer categoryId,
            @Query("available") Boolean available
    );

    @GET("api/v1/products/{id}")
    Call<ProductEntity> getProductById(@Path("id") int id);

    @POST("api/v1/products")
    Call<ProductEntity> createProduct(@Body ProductEntity product);

    @PUT("api/v1/products/{id}")
    Call<ProductEntity> updateProduct(@Path("id") int id, @Body ProductEntity product);

    @DELETE("api/v1/products/{id}")
    Call<Void> deleteProduct(@Path("id") int id);
}
