package com.example.cafe_manager.data.remote;

import android.content.Context;
import android.content.Intent;
import com.example.cafe_manager.manager.SessionManager;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Interceptor để tự động đính kèm JWT Token vào Header của các request
 * và bắt lỗi 401 Unauthorized để thực hiện đăng xuất cưỡng bức.
 */
public class AuthInterceptor implements Interceptor {
    private final Context context;
    private final SessionManager sessionManager;

    public AuthInterceptor(Context context) {
        this.context = context.getApplicationContext();
        this.sessionManager = SessionManager.getInstance(context);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request.Builder builder = originalRequest.newBuilder();

        String token = sessionManager.getToken();
        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }

        Request request = builder.build();
        Response response = chain.proceed(request);

        if (response.code() == 401) {
            // Token hết hạn hoặc không hợp lệ -> xóa session
            sessionManager.logout();
            
            // Gửi broadcast để các Activity nhận biết và quay về màn hình đăng nhập
            Intent intent = new Intent("com.example.cafe_manager.ACTION_LOGOUT");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.sendBroadcast(intent);
        }

        return response;
    }
}
