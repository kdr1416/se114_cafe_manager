package com.example.cafe_manager.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.local.entity.AttendanceEntity;
import com.example.cafe_manager.data.remote.ApiClient;
import com.example.cafe_manager.data.remote.AttendanceApiService;
import com.example.cafe_manager.data.remote.AttendanceResponse;
import com.example.cafe_manager.data.remote.CheckInRequest;
import com.example.cafe_manager.data.remote.CheckOutRequest;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.RepositoryCallback;

import java.util.ArrayList;
import java.util.List;

public class AttendanceRepository {

    private static volatile AttendanceRepository instance;
    private final AttendanceApiService apiService;
    private final AppExecutors exec;
    private final MutableLiveData<List<AttendanceEntity>> allAttendancesCache = new MutableLiveData<>();

    private AttendanceRepository(Context ctx) {
        this.apiService = ApiClient.getInstance(ctx).getService(AttendanceApiService.class);
        this.exec = AppExecutors.getInstance();
        refreshAttendances();
    }

    public static AttendanceRepository getInstance(Context ctx) {
        if (instance == null) {
            synchronized (AttendanceRepository.class) {
                if (instance == null) {
                    instance = new AttendanceRepository(ctx.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public LiveData<List<AttendanceEntity>> getAttendanceForShift(int shiftId) {
        MutableLiveData<List<AttendanceEntity>> result = new MutableLiveData<>();
        exec.diskIO().execute(() -> {
            try {
                retrofit2.Response<List<AttendanceResponse>> response = apiService.getAttendancesForShift(shiftId).execute();
                if (response.isSuccessful() && response.body() != null) {
                    List<AttendanceEntity> entities = new ArrayList<>();
                    for (AttendanceResponse r : response.body()) {
                        entities.add(mapResponseToEntity(r));
                    }
                    result.postValue(entities);
                } else {
                    result.postValue(new ArrayList<>());
                }
            } catch (Exception e) {
                result.postValue(new ArrayList<>());
            }
        });
        return result;
    }

    public LiveData<List<AttendanceEntity>> getMyAttendance(int userId) {
        // Note: userId is ignored; API returns current user's attendance only
        return allAttendancesCache;
    }

    public void checkIn(int shiftId, int userId, RepositoryCallback<Void> callback) {
        CheckInRequest request = new CheckInRequest();
        request.setShiftId(shiftId);
        // latitude/longitude not provided in current flow, send null
        exec.diskIO().execute(() -> {
            try {
                retrofit2.Response<AttendanceResponse> response = apiService.checkIn(request).execute();
                if (response.isSuccessful()) {
                    refreshAttendances();
                    exec.mainThread().execute(() -> callback.onSuccess(null));
                } else {
                    exec.mainThread().execute(() -> callback.onError(new Exception("Check-in thất bại")));
                }
            } catch (Exception e) {
                exec.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void checkOut(int shiftId, int userId, RepositoryCallback<Void> callback) {
        CheckOutRequest request = new CheckOutRequest();
        request.setShiftId(shiftId);
        // notes not provided in current flow, send null
        exec.diskIO().execute(() -> {
            try {
                retrofit2.Response<AttendanceResponse> response = apiService.checkOut(request).execute();
                if (response.isSuccessful()) {
                    refreshAttendances();
                    exec.mainThread().execute(() -> callback.onSuccess(null));
                } else {
                    exec.mainThread().execute(() -> callback.onError(new Exception("Check-out thất bại")));
                }
            } catch (Exception e) {
                exec.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    private void refreshAttendances() {
        exec.diskIO().execute(() -> {
            try {
                retrofit2.Response<List<AttendanceResponse>> response = apiService.getAllAttendances().execute();
                if (response.isSuccessful() && response.body() != null) {
                    List<AttendanceEntity> entities = new ArrayList<>();
                    for (AttendanceResponse r : response.body()) {
                        entities.add(mapResponseToEntity(r));
                    }
                    allAttendancesCache.postValue(entities);
                }
            } catch (Exception e) {
                // Log error
            }
        });
    }

    private AttendanceEntity mapResponseToEntity(AttendanceResponse r) {
        AttendanceEntity e = new AttendanceEntity();
        e.setAttendanceId(r.getAttendanceId());
        e.setShiftId(r.getShiftId());
        e.setUserId(r.getUserId() != null ? r.getUserId() : 0);
        e.setCheckInAt(r.getCheckInAt() != null ? r.getCheckInAt() : 0);
        e.setCheckOutAt(r.getCheckOutAt() != null ? r.getCheckOutAt() : 0);
        e.setStatus(r.getStatus());
        e.setLateMinutes(r.getLateMinutes() != null ? r.getLateMinutes() : 0);
        e.setEarlyLeaveMinutes(r.getEarlyLeaveMinutes() != null ? r.getEarlyLeaveMinutes() : 0);
        e.setNotes(r.getNotes());
        return e;
    }
}
