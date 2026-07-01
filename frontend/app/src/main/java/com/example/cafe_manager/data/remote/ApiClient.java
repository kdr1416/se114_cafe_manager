package com.example.cafe_manager.data.remote;

import android.content.Context;
import com.example.cafe_manager.BuildConfig;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.io.IOException;

/**
 * Singleton quản lý cấu hình và cung cấp Retrofit client.
 * Hỗ trợ cấu hình IP máy chủ động để chuyển đổi mạng dễ dàng mà không cần build lại app.
 */
public class ApiClient {
    private static volatile ApiClient instance;
    private final Retrofit retrofit;

    private ApiClient(Context context) {
        // Cấu hình logging interceptor cho debug
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Cấu hình OkHttpClient đính kèm AuthInterceptor, DynamicUrlInterceptor và LoggingInterceptor
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .addInterceptor(new AuthInterceptor(context))
                .addInterceptor(new DynamicUrlInterceptor(context))
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

    /**
     * Lấy URL hoàn chỉnh của máy chủ (dùng cho cả HTTP và WebSocket).
     */
    public static String getServerUrl(Context context) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("cafe_manager_prefs", Context.MODE_PRIVATE);
        String savedIp = prefs.getString("server_ip", null);
        if (savedIp == null || savedIp.trim().isEmpty()) {
            return BuildConfig.BASE_URL;
        }
        
        String url = savedIp.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        if (!url.endsWith("/")) {
            url = url + "/";
        }
        return url;
    }

    /**
     * Lấy địa chỉ IP/Host thô (ví dụ: "192.168.1.46:8080").
     */
    public static String getServerIp(Context context) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("cafe_manager_prefs", Context.MODE_PRIVATE);
        String savedIp = prefs.getString("server_ip", null);
        if (savedIp == null || savedIp.trim().isEmpty()) {
            String url = BuildConfig.BASE_URL;
            url = url.replace("http://", "").replace("https://", "");
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }
            return url;
        }
        return savedIp.trim();
    }

    /**
     * Lưu địa chỉ IP/Host mới.
     */
    public static void setServerIp(Context context, String ip) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("cafe_manager_prefs", Context.MODE_PRIVATE);
        prefs.edit().putString("server_ip", ip).apply();
    }

    /**
     * Interceptor cấu hình lại địa chỉ URL của request dựa trên IP đã được lưu trong máy.
     */
    private static class DynamicUrlInterceptor implements Interceptor {
        private final Context context;

        public DynamicUrlInterceptor(Context context) {
            this.context = context.getApplicationContext();
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            String savedIp = getServerIp(context);
            
            if (savedIp != null && !savedIp.isEmpty()) {
                String host = savedIp;
                int port = 8080;
                
                if (savedIp.contains(":")) {
                    String[] parts = savedIp.split(":");
                    host = parts[0];
                    try {
                        port = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException ignored) {}
                }
                
                okhttp3.HttpUrl.Builder newUrlBuilder = request.url().newBuilder()
                        .host(host);
                
                if (savedIp.contains(":")) {
                    newUrlBuilder.port(port);
                }
                
                request = request.newBuilder()
                        .url(newUrlBuilder.build())
                        .build();
            }
            
            return chain.proceed(request);
        }
    }
}
