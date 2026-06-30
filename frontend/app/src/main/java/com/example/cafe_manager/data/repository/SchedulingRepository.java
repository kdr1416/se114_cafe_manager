package com.example.cafe_manager.data.repository;

import android.content.Context;
import android.util.Log;

import com.example.cafe_manager.data.remote.ApiClient;
import com.example.cafe_manager.data.remote.SchedulingApiService;
import com.example.cafe_manager.data.remote.SchedulingRequest;
import com.example.cafe_manager.data.remote.SchedulingResponse;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.RepositoryCallback;

import java.util.concurrent.TimeUnit;

import retrofit2.Response;
import retrofit2.Call;

public class SchedulingRepository {

    private static volatile SchedulingRepository instance;
    private final SchedulingApiService apiService;
    private final AppExecutors exec;

    private SchedulingRepository(Context ctx) {
        this.apiService = ApiClient.getInstance(ctx).getService(SchedulingApiService.class);
        this.exec = AppExecutors.getInstance();
    }

    public static SchedulingRepository getInstance(Context ctx) {
        if (instance == null) {
            synchronized (SchedulingRepository.class) {
                if (instance == null) {
                    instance = new SchedulingRepository(ctx.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public void runPreview(Long startDate, Long endDate, RepositoryCallback<SchedulingResponse> callback) {
        exec.diskIO().execute(() -> {
            Log.d("SchedulingRepo", "runPreview START: " + System.currentTimeMillis());
            try {
                SchedulingRequest request = new SchedulingRequest();
                request.setStartDate(startDate);
                request.setEndDate(endDate);

                Response<SchedulingResponse> response = apiService.runPreview(request).execute();
                Log.d("SchedulingRepo", "runPreview RESPONSE received: " + System.currentTimeMillis()
                    + " code=" + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("SchedulingRepo", "runPreview SUCCESS, suggestions count: "
                        + (response.body().getSuggestions() != null ? response.body().getSuggestions().size() : 0));
                    exec.mainThread().execute(() -> callback.onSuccess(response.body()));
                } else {
                    Log.d("SchedulingRepo", "runPreview FAILED: " + response.code());
                    String errorMsg = parseErrorMessage(response);
                    exec.mainThread().execute(() -> callback.onError(new Exception(errorMsg)));
                }
            } catch (Exception e) {
                Log.d("SchedulingRepo", "runPreview EXCEPTION: " + e.getMessage());
                exec.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void getPreview(long runId, RepositoryCallback<SchedulingResponse> callback) {
        exec.diskIO().execute(() -> {
            try {
                Response<SchedulingResponse> response = apiService.getPreview(runId).execute();
                if (response.isSuccessful() && response.body() != null) {
                    exec.mainThread().execute(() -> callback.onSuccess(response.body()));
                } else {
                    String errorMsg = parseErrorMessage(response);
                    exec.mainThread().execute(() -> callback.onError(new Exception(errorMsg)));
                }
            } catch (Exception e) {
                exec.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void applyPreview(long runId, RepositoryCallback<SchedulingResponse> callback) {
        exec.diskIO().execute(() -> {
            try {
                Response<SchedulingResponse> response = apiService.applyPreview(runId).execute();
                if (response.isSuccessful() && response.body() != null) {
                    exec.mainThread().execute(() -> callback.onSuccess(response.body()));
                } else {
                    String errorMsg = parseErrorMessage(response);
                    exec.mainThread().execute(() -> callback.onError(new Exception(errorMsg)));
                }
            } catch (Exception e) {
                exec.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    private String parseErrorMessage(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String body = response.errorBody().string();
                Log.e("SchedulingRepository", "Error body: " + body);
                // Try parse {"message":"..."}
                if (body.contains("\"message\"")) {
                    int idx = body.indexOf("\"message\"");
                    int colon = body.indexOf(":", idx);
                    int quote1 = body.indexOf("\"", colon + 1);
                    int quote2 = body.indexOf("\"", quote1 + 1);
                    if (quote1 > 0 && quote2 > quote1) {
                        return body.substring(quote1 + 1, quote2);
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return "Lỗi server: " + response.code();
    }
}
