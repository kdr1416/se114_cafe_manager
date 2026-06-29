package com.example.cafe_manager.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.entity.AttendanceEntity;
import com.example.cafe_manager.data.remote.ApiClient;
import com.example.cafe_manager.data.remote.AttendanceApiService;
import com.example.cafe_manager.data.remote.AttendanceResponse;
import com.example.cafe_manager.data.remote.CheckInRequest;
import com.example.cafe_manager.data.remote.CheckOutRequest;
import com.example.cafe_manager.data.remote.UserAttendanceDetailResponse;
import com.example.cafe_manager.data.local.entity.ShiftEntity;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.RepositoryCallback;

import java.util.ArrayList;
import java.util.List;

public class AttendanceRepository {

    private static volatile AttendanceRepository instance;
    private final AttendanceApiService apiService;
    private final AppExecutors exec;
    private final Context appContext;
    private final MutableLiveData<List<AttendanceEntity>> allAttendancesCache = new MutableLiveData<>();

    private AttendanceRepository(Context ctx) {
        this.appContext = ctx.getApplicationContext();
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
                AppDatabase db = AppDatabase.getInstance(appContext);
                ShiftEntity shift = db.shiftDao().getById(shiftId);
                if (shift != null && (Constants.SHIFT_DRAFT.equals(shift.getStatus())
                        || Constants.SHIFT_CANCELLED.equals(shift.getStatus()))) {
                    exec.mainThread().execute(() -> callback.onError(new Exception("Không thể chấm công cho ca tạm hoặc ca đã hủy")));
                    return;
                }
                retrofit2.Response<AttendanceResponse> response = apiService.checkIn(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    AttendanceResponse r = response.body();
                    AttendanceEntity entity = mapResponseToEntity(r);
                    AttendanceEntity existing = db.attendanceDao().getByShiftAndUser(entity.getShiftId(), entity.getUserId());
                    if (existing == null) {
                        db.attendanceDao().insert(entity);
                    } else {
                        entity.setAttendanceId(existing.getAttendanceId());
                        db.attendanceDao().update(entity);
                    }
                    
                    refreshAttendances();
                    exec.mainThread().execute(() -> callback.onSuccess(null));
                } else {
                    Exception err = parseError(response, "Check-in thất bại");
                    exec.mainThread().execute(() -> callback.onError(err));
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
                AppDatabase db = AppDatabase.getInstance(appContext);
                ShiftEntity shift = db.shiftDao().getById(shiftId);
                if (shift != null && (Constants.SHIFT_DRAFT.equals(shift.getStatus())
                        || Constants.SHIFT_CANCELLED.equals(shift.getStatus()))) {
                    exec.mainThread().execute(() -> callback.onError(new Exception("Không thể chấm công cho ca tạm hoặc ca đã hủy")));
                    return;
                }
                retrofit2.Response<AttendanceResponse> response = apiService.checkOut(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    AttendanceResponse r = response.body();
                    AttendanceEntity entity = mapResponseToEntity(r);
                    AttendanceEntity existing = db.attendanceDao().getByShiftAndUser(entity.getShiftId(), entity.getUserId());
                    if (existing == null) {
                        db.attendanceDao().insert(entity);
                    } else {
                        entity.setAttendanceId(existing.getAttendanceId());
                        db.attendanceDao().update(entity);
                    }
                    
                    refreshAttendances();
                    exec.mainThread().execute(() -> callback.onSuccess(null));
                } else {
                    Exception err = parseError(response, "Check-out thất bại");
                    exec.mainThread().execute(() -> callback.onError(err));
                }
            } catch (Exception e) {
                exec.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void syncMyAttendances(RepositoryCallback<Void> callback) {
        exec.diskIO().execute(() -> {
            try {
                retrofit2.Response<List<AttendanceResponse>> response = apiService.getAllAttendances().execute();
                if (response.isSuccessful() && response.body() != null) {
                    List<AttendanceEntity> entities = new ArrayList<>();
                    AppDatabase db = AppDatabase.getInstance(appContext);
                    db.runInTransaction(() -> {
                        for (AttendanceResponse r : response.body()) {
                            AttendanceEntity entity = mapResponseToEntity(r);
                            AttendanceEntity existing = db.attendanceDao().getByShiftAndUser(entity.getShiftId(), entity.getUserId());
                            if (existing == null) {
                                db.attendanceDao().insert(entity);
                            } else {
                                entity.setAttendanceId(existing.getAttendanceId());
                                db.attendanceDao().update(entity);
                            }
                            entities.add(entity);
                        }
                    });
                    allAttendancesCache.postValue(entities);
                    if (callback != null) {
                        exec.mainThread().execute(() -> callback.onSuccess(null));
                    }
                } else {
                    if (callback != null) {
                        Exception err = parseError(response, "Đồng bộ điểm danh thất bại");
                        exec.mainThread().execute(() -> callback.onError(err));
                    }
                }
            } catch (Exception e) {
                if (callback != null) {
                    exec.mainThread().execute(() -> callback.onError(e));
                }
            }
        });
    }

    private void refreshAttendances() {
        syncMyAttendances(null);
    }

    private Exception parseError(retrofit2.Response<?> response, String defaultMsg) {
        try {
            if (response.errorBody() != null) {
                String errorStr = response.errorBody().string();
                org.json.JSONObject json = new org.json.JSONObject(errorStr);
                if (json.has("message")) {
                    return new Exception(json.getString("message"));
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return new Exception(defaultMsg);
    }

    public void getUserDetails(int userId, int year, int month, RepositoryCallback<UserAttendanceDetailResponse> callback) {
        exec.diskIO().execute(() -> {
            try {
                retrofit2.Response<UserAttendanceDetailResponse> response = apiService.getUserDetails(userId, year, month).execute();
                if (response.isSuccessful() && response.body() != null) {
                    exec.mainThread().execute(() -> callback.onSuccess(response.body()));
                } else {
                    Exception err = parseError(response, "Lấy báo cáo chi tiết thất bại");
                    exec.mainThread().execute(() -> callback.onError(err));
                }
            } catch (Exception e) {
                exec.mainThread().execute(() -> callback.onError(e));
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
