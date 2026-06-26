package com.example.cafe_manager.data.remote;

import android.content.Context;
import com.example.cafe_manager.BuildConfig;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton quản lý cấu hình và cung cấp Retrofit client.
 */
public class ApiClient {
    private static volatile ApiClient instance;
    private final Retrofit retrofit;

    private ApiClient(Context context) {
        // Cấu hình logging interceptor cho debug
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Cấu hình OkHttpClient đính kèm AuthInterceptor và LoggingInterceptor
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .addInterceptor(new AuthInterceptor(context))
                .addInterceptor(logging)
                .build();

        // Khởi tạo Retrofit
        retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static ApiClient getInstance(Context context) {
        if (instance == null) {
            synchronized (ApiClient.class) {
                if (instance == null) {
                    instance = new ApiClient(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    /**
     * Tạo service instance cho Retrofit interface.
     */
    public <T> T getService(Class<T> serviceClass) {
        return retrofit.create(serviceClass);
    }
}
