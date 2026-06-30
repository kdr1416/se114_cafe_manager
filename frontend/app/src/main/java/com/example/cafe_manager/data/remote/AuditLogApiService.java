package com.example.cafe_manager.data.remote;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Retrofit interface kết nối đến AuditLogController backend.
 */
public interface AuditLogApiService {
    @POST("api/v1/audit-logs")
    Call<AuditLogResponse> createLog(@Body CreateAuditLogRequest request);
}
