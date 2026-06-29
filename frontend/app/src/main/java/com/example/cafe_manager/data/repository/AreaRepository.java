package com.example.cafe_manager.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.local.entity.AreaEntity;
import com.example.cafe_manager.data.remote.ApiClient;
import com.example.cafe_manager.data.remote.AreaApiService;
import com.example.cafe_manager.data.remote.AreaResponse;
import com.example.cafe_manager.data.remote.CreateAreaRequest;
import com.example.cafe_manager.data.remote.UpdateAreaRequest;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.RepositoryCallback;

import java.util.ArrayList;
import java.util.List;

public class AreaRepository {
    private static volatile AreaRepository instance;
    private final AreaApiService apiService;
    private final AppExecutors exec;
    private final MutableLiveData<List<AreaEntity>> allAreasCache = new MutableLiveData<>();

    private AreaRepository(Context ctx) {
        this.apiService = ApiClient.getInstance(ctx).getService(AreaApiService.class);
        this.exec = AppExecutors.getInstance();
        refreshAreas();
    }

    public static AreaRepository getInstance(Context ctx) {
        if (instance == null) {
            synchronized (AreaRepository.class) {
                if (instance == null) {
                    instance = new AreaRepository(ctx.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public LiveData<List<AreaEntity>> getAllAreas() {
        return allAreasCache;
    }

    public void getAreaById(int areaId, RepositoryCallback<AreaEntity> callback) {
        exec.diskIO().execute(() -> {
            try {
                retrofit2.Response<AreaResponse> response = apiService.getAreaById(areaId).execute();
                if (response.isSuccessful() && response.body() != null) {
                    AreaEntity entity = mapResponseToEntity(response.body());
                    exec.mainThread().execute(() -> callback.onSuccess(entity));
                } else {
                    exec.mainThread().execute(() -> callback.onError(new Exception("Không tìm thấy khu vực")));
                }
            } catch (Exception e) {
                exec.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void insert(AreaEntity area, Runnable onSuccess) {
        CreateAreaRequest request = new CreateAreaRequest();
        request.setAreaName(area.getAreaName());
        request.setPrefix(area.getPrefix());
        request.setDescription(area.getDescription());

        exec.diskIO().execute(() -> {
            try {
                retrofit2.Response<AreaResponse> response = apiService.createArea(request).execute();
                if (response.isSuccessful()) {
                    refreshAreas();
                    exec.mainThread().execute(onSuccess);
                } else {
                    exec.mainThread().execute(onSuccess);
                }
            } catch (Exception e) {
                exec.mainThread().execute(onSuccess);
            }
        });
    }

    public void update(AreaEntity area, Runnable onSuccess) {
        UpdateAreaRequest request = new UpdateAreaRequest();
        request.setAreaName(area.getAreaName());
        request.setPrefix(area.getPrefix());
        request.setDescription(area.getDescription());

        exec.diskIO().execute(() -> {
            try {
                retrofit2.Response<AreaResponse> response = apiService.updateArea(area.getAreaId(), request).execute();
                if (response.isSuccessful()) {
                    refreshAreas();
                    exec.mainThread().execute(onSuccess);
                } else {
                    exec.mainThread().execute(onSuccess);
                }
            } catch (Exception e) {
                exec.mainThread().execute(onSuccess);
            }
        });
    }

    public void delete(int areaId, String areaName, Runnable onSuccess, Runnable onError) {
        exec.diskIO().execute(() -> {
            try {
                retrofit2.Response<Void> response = apiService.deleteArea(areaId).execute();
                if (response.isSuccessful()) {
                    refreshAreas();
                    exec.mainThread().execute(onSuccess);
                } else {
                    exec.mainThread().execute(onError);
                }
            } catch (Exception e) {
                exec.mainThread().execute(onError);
            }
        });
    }

    private void refreshAreas() {
        exec.diskIO().execute(() -> {
            try {
                retrofit2.Response<List<AreaResponse>> response = apiService.getAllAreas().execute();
                if (response.isSuccessful() && response.body() != null) {
                    List<AreaEntity> entities = new ArrayList<>();
                    for (AreaResponse r : response.body()) {
                        entities.add(mapResponseToEntity(r));
                    }
                    allAreasCache.postValue(entities);
                }
            } catch (Exception e) {
                // Log error
            }
        });
    }

    private AreaEntity mapResponseToEntity(AreaResponse r) {
        AreaEntity e = new AreaEntity();
        e.setAreaId(r.getAreaId());
        e.setAreaName(r.getAreaName());
        e.setPrefix(r.getPrefix());
        e.setDescription(r.getDescription());
        e.setTableCount(r.getTableCount());
        // createdAt is not in response; set to 0 or current time
        e.setCreatedAt(System.currentTimeMillis());
        return e;
    }
}