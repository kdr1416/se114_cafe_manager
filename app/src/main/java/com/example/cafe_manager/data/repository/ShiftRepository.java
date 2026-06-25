package com.example.cafe_manager.data.repository;

import android.app.Application;
import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.local.entity.ShiftAssignmentEntity;
import com.example.cafe_manager.data.local.entity.ShiftEntity;
import com.example.cafe_manager.data.local.entity.ShiftTemplateEntity;
import com.example.cafe_manager.data.remote.ApiClient;
import com.example.cafe_manager.data.remote.AssignStaffRequest;
import com.example.cafe_manager.data.remote.CloseShiftRequest;
import com.example.cafe_manager.data.remote.CreateShiftRequest;
import com.example.cafe_manager.data.remote.OpenShiftRequest;
import com.example.cafe_manager.data.remote.ShiftAssignmentResponse;
import com.example.cafe_manager.data.remote.ShiftApiService;
import com.example.cafe_manager.data.remote.ShiftResponse;
import com.example.cafe_manager.data.remote.ShiftTemplateResponse;
import com.example.cafe_manager.data.remote.UpdateShiftRequest;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.RepositoryCallback;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Response;

public class ShiftRepository {
    private static volatile ShiftRepository instance;
    private final ShiftApiService apiService;
    private final AppExecutors exec;

    // Caches
    private final MutableLiveData<List<ShiftTemplateEntity>> allTemplatesCache = new MutableLiveData<>();
    private final MediatorLiveData<List<ShiftTemplateEntity>> activeTemplatesCache = new MediatorLiveData<>();
    private final MutableLiveData<List<ShiftEntity>> allShiftsCache = new MutableLiveData<>();

    private ShiftRepository(Context ctx) {
        this.apiService = ApiClient.getInstance(ctx).getService(ShiftApiService.class);
        this.exec = AppExecutors.getInstance();
        // Setup active templates filter
        activeTemplatesCache.addSource(allTemplatesCache, list -> {
            if (list == null) {
                activeTemplatesCache.setValue(null);
                return;
            }
            List<ShiftTemplateEntity> active = new ArrayList<>();
            for (ShiftTemplateEntity t : list) {
                if (t.isActive()) {
                    active.add(t);
                }
            }
            activeTemplatesCache.setValue(active);
        });
        // Initial refresh
        refreshTemplates();
        refreshShifts();
    }

    public static ShiftRepository getInstance(Context ctx) {
        if (instance == null) {
            synchronized (ShiftRepository.class) {
                if (instance == null) {
                    instance = new ShiftRepository(ctx.getApplicationContext());
                }
            }
        }
        return instance;
    }

    // ── Templates ──
    public LiveData<List<ShiftTemplateEntity>> getAllTemplates() {
        return allTemplatesCache;
    }

    public LiveData<List<ShiftTemplateEntity>> getActiveTemplates() {
        return activeTemplatesCache;
    }

    public void insertTemplate(ShiftTemplateEntity template, RepositoryCallback<Long> callback) {
        exec.diskIO().execute(() -> {
            try {
                ShiftTemplateResponse request = new ShiftTemplateResponse();
                request.setTemplateName(template.getTemplateName());
                request.setStartTime(template.getStartTime());
                request.setEndTime(template.getEndTime());
                request.setMinStaff(template.getMinStaff());
                request.setActive(template.isActive());
                retrofit2.Response<ShiftTemplateResponse> response = apiService.createTemplate(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    refreshTemplates();
                    exec.mainThread().execute(() -> callback.onSuccess((long) response.body().getTemplateId()));
                } else {
                    exec.mainThread().execute(() -> callback.onError(new Exception("Không thể tạo mẫu ca")));
                }
            } catch (Exception e) {
                exec.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void updateTemplate(ShiftTemplateEntity template, RepositoryCallback<Void> callback) {
        exec.diskIO().execute(() -> {
            try {
                ShiftTemplateResponse request = new ShiftTemplateResponse();
                request.setTemplateName(template.getTemplateName());
                request.setStartTime(template.getStartTime());
                request.setEndTime(template.getEndTime());
                request.setMinStaff(template.getMinStaff());
                request.setActive(template.isActive());
                retrofit2.Response<ShiftTemplateResponse> response = apiService.updateTemplate(template.getTemplateId(), request).execute();
                if (response.isSuccessful()) {
                    refreshTemplates();
                    exec.mainThread().execute(() -> callback.onSuccess(null));
                } else {
                    exec.mainThread().execute(() -> callback.onError(new Exception("Không thể cập nhật mẫu ca")));
                }
            } catch (Exception e) {
                exec.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void deactivateTemplate(int templateId, RepositoryCallback<Void> callback) {
        exec.diskIO().execute(() -> {
            try {
                retrofit2.Response<Void> response = apiService.deactivateTemplate(templateId).execute();
                if (response.isSuccessful()) {
                    refreshTemplates();
                    exec.mainThread().execute(() -> callback.onSuccess(null));
                } else {
                    exec.mainThread().execute(() -> callback.onError(new Exception("Không thể ngưng hoạt động mẫu ca")));
                }
            } catch (Exception e) {
                exec.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    // ── Shifts ──
    public LiveData<List<ShiftEntity>> getShiftsByDate(long date) {
        android.util.Log.d("ShiftRepository", "getShiftsByDate called for date: " + date + " (" + new java.util.Date(date) + ")");
        MediatorLiveData<List<ShiftEntity>> result = new MediatorLiveData<>();
        
        java.util.Calendar calTarget = java.util.Calendar.getInstance();
        calTarget.setTimeInMillis(date);
        int targetYear = calTarget.get(java.util.Calendar.YEAR);
        int targetMonth = calTarget.get(java.util.Calendar.MONTH);
        int targetDay = calTarget.get(java.util.Calendar.DAY_OF_MONTH);

        result.addSource(allShiftsCache, shifts -> {
            if (shifts == null) {
                android.util.Log.d("ShiftRepository", "getShiftsByDate: shifts cache is null");
                result.setValue(null);
                return;
            }
            List<ShiftEntity> filtered = new ArrayList<>();
            java.util.Calendar calShift = java.util.Calendar.getInstance();
            for (ShiftEntity s : shifts) {
                calShift.setTimeInMillis(s.getShiftDate());
                boolean sameDay = calShift.get(java.util.Calendar.YEAR) == targetYear &&
                                  calShift.get(java.util.Calendar.MONTH) == targetMonth &&
                                  calShift.get(java.util.Calendar.DAY_OF_MONTH) == targetDay;
                
                android.util.Log.d("ShiftRepository", "Shift in cache: ID=" + s.getShiftId() + ", Name=" + s.getShiftName() + ", Date=" + s.getShiftDate() + " (" + new java.util.Date(s.getShiftDate()) + "), sameDay=" + sameDay);
                if (sameDay) {
                    filtered.add(s);
                }
            }
            android.util.Log.d("ShiftRepository", "getShiftsByDate returning " + filtered.size() + " shifts matching " + date);
            result.setValue(filtered);
        });
        return result;
    }

    public void insertShift(ShiftEntity shift, RepositoryCallback<Long> callback) {
        exec.diskIO().execute(() -> {
            try {
                CreateShiftRequest request = new CreateShiftRequest();
                request.setTemplateId(shift.getTemplateId());
                request.setShiftName(shift.getShiftName());
                request.setShiftDate(shift.getShiftDate());
                request.setStartTime(shift.getStartTime());
                request.setEndTime(shift.getEndTime());
                retrofit2.Response<ShiftResponse> response = apiService.createShift(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    refreshShifts();
                    exec.mainThread().execute(() -> callback.onSuccess((long) response.body().getShiftId()));
                } else {
                    exec.mainThread().execute(() -> callback.onError(new Exception("Không thể tạo ca")));
                }
            } catch (Exception e) {
                exec.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void insertShiftNoRefresh(ShiftEntity shift, RepositoryCallback<Long> callback) {
        exec.diskIO().execute(() -> {
            try {
                CreateShiftRequest request = new CreateShiftRequest();
                request.setTemplateId(shift.getTemplateId());
                request.setShiftName(shift.getShiftName());
                request.setShiftDate(shift.getShiftDate());
                request.setStartTime(shift.getStartTime());
                request.setEndTime(shift.getEndTime());
                retrofit2.Response<ShiftResponse> response = apiService.createShift(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    exec.mainThread().execute(() -> callback.onSuccess((long) response.body().getShiftId()));
                } else {
                    exec.mainThread().execute(() -> callback.onError(new Exception("Không thể tạo ca")));
                }
            } catch (Exception e) {
                exec.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void openShiftWithCash(int shiftId, double openingCash, int userId, RepositoryCallback<Void> callback) {
        exec.diskIO().execute(() -> {
            try {
                OpenShiftRequest request = new OpenShiftRequest();
                request.setOpeningCash(openingCash);
                retrofit2.Response<ShiftResponse> response = apiService.openShift(shiftId, request).execute();
                if (response.isSuccessful()) {
                    refreshShifts();
                    exec.mainThread().execute(() -> callback.onSuccess(null));
                } else {
                    exec.mainThread().execute(() -> callback.onError(new Exception("Không thể mở ca")));
                }
            } catch (Exception e) {
                exec.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void publishShift(int shiftId, RepositoryCallback<Void> callback) {
        exec.diskIO().execute(() -> {
            try {
                retrofit2.Response<ShiftResponse> response = apiService.publishShift(shiftId).execute();
                if (response.isSuccessful()) {
                    refreshShifts();
                    exec.mainThread().execute(() -> callback.onSuccess(null));
                } else {
                    exec.mainThread().execute(() -> callback.onError(new Exception("Không thể phát hành ca: " + response.code())));
                }
            } catch (Exception e) {
                exec.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void cancelShift(int shiftId, RepositoryCallback<Void> callback) {
        exec.diskIO().execute(() -> {
            try {
                retrofit2.Response<ShiftResponse> response = apiService.cancelShift(shiftId).execute();
                if (response.isSuccessful()) {
                    refreshShifts();
                    exec.mainThread().execute(() -> callback.onSuccess(null));
                } else {
                    exec.mainThread().execute(() -> callback.onError(new Exception("Không thể hủy ca: " + response.code())));
                }
            } catch (Exception e) {
                exec.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void assignStaff(ShiftAssignmentEntity assignment, RepositoryCallback<Long> callback) {
        exec.diskIO().execute(() -> {
            try {
                AssignStaffRequest request = new AssignStaffRequest();
                request.setUserId(assignment.getUserId());
                request.setRole(assignment.getRole());
                retrofit2.Response<Void> response = apiService.assignStaff(assignment.getShiftId(), request).execute();
                if (response.isSuccessful()) {
                    exec.mainThread().execute(() -> callback.onSuccess(null));
                } else {
                    exec.mainThread().execute(() -> callback.onError(new Exception("Không thể phân công nhân viên")));
                }
            } catch (Exception e) {
                exec.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void removeAssignment(int assignmentId, RepositoryCallback<Void> callback) {
        exec.diskIO().execute(() -> {
            try {
                // First get assignment to obtain shiftId and userId
                retrofit2.Response<ShiftAssignmentResponse> getResp = apiService.getAssignment(assignmentId).execute();
                if (!getResp.isSuccessful() || getResp.body() == null) {
                    exec.mainThread().execute(() -> callback.onError(new Exception("Không tìm thấy phân công")));
                    return;
                }
                ShiftAssignmentResponse assignment = getResp.body();
                // Then delete
                retrofit2.Response<Void> delResp = apiService.unassignStaff(assignment.getShiftId(), assignment.getUserId()).execute();
                if (delResp.isSuccessful()) {
                    exec.mainThread().execute(() -> callback.onSuccess(null));
                } else {
                    exec.mainThread().execute(() -> callback.onError(new Exception("Không thể hủy phân công")));
                }
            } catch (Exception e) {
                exec.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void confirmAssignment(int assignmentId, RepositoryCallback<Void> callback) {
        exec.diskIO().execute(() -> {
            try {
                retrofit2.Response<Void> response = apiService.confirmAssignment(assignmentId).execute();
                if (response.isSuccessful()) {
                    exec.mainThread().execute(() -> callback.onSuccess(null));
                } else {
                    exec.mainThread().execute(() -> callback.onError(new Exception("Không thể xác nhận phân công")));
                }
            } catch (Exception e) {
                exec.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void getShiftById(int shiftId, RepositoryCallback<ShiftEntity> callback) {
        exec.diskIO().execute(() -> {
            try {
                retrofit2.Response<ShiftResponse> response = apiService.getShiftById(shiftId).execute();
                if (response.isSuccessful() && response.body() != null) {
                    ShiftEntity entity = mapShiftResponseToEntity(response.body());
                    exec.mainThread().execute(() -> callback.onSuccess(entity));
                } else {
                    exec.mainThread().execute(() -> callback.onError(new Exception("Không thể tải thông tin ca: " + response.code())));
                }
            } catch (Exception e) {
                exec.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    // ── Refresh helpers ──
    public void refreshTemplatesFromApi(RepositoryCallback<Void> callback) {
        exec.diskIO().execute(() -> {
            try {
                retrofit2.Response<List<ShiftTemplateResponse>> response = apiService.getAllTemplates().execute();
                if (response.isSuccessful() && response.body() != null) {
                    List<ShiftTemplateEntity> entities = new ArrayList<>();
                    for (ShiftTemplateResponse r : response.body()) {
                        entities.add(mapTemplateResponseToEntity(r));
                    }
                    allTemplatesCache.postValue(entities);
                    if (callback != null) {
                        exec.mainThread().execute(() -> callback.onSuccess(null));
                    }
                } else {
                    if (callback != null) {
                        exec.mainThread().execute(() -> callback.onError(new Exception("Lỗi tải mẫu ca: " + response.code())));
                    }
                }
            } catch (Exception e) {
                if (callback != null) {
                    exec.mainThread().execute(() -> callback.onError(e));
                }
            }
        });
    }

    private void refreshTemplates() {
        refreshTemplatesFromApi(null);
    }

    public void refreshShiftsFromApi(RepositoryCallback<Void> callback) {
        android.util.Log.d("ShiftRepository", "refreshShiftsFromApi called");
        exec.diskIO().execute(() -> {
            try {
                retrofit2.Response<List<ShiftResponse>> response = apiService.getShifts(null, null).execute();
                if (response.isSuccessful() && response.body() != null) {
                    List<ShiftEntity> entities = new ArrayList<>();
                    for (ShiftResponse r : response.body()) {
                        entities.add(mapShiftResponseToEntity(r));
                    }
                    android.util.Log.d("ShiftRepository", "refreshShiftsFromApi success. Fetched " + entities.size() + " shifts.");
                    allShiftsCache.postValue(entities);
                    if (callback != null) {
                        exec.mainThread().execute(() -> callback.onSuccess(null));
                    }
                } else {
                    android.util.Log.e("ShiftRepository", "refreshShiftsFromApi failed. Response code: " + response.code());
                    if (callback != null) {
                        exec.mainThread().execute(() -> callback.onError(new Exception("Lỗi tải ca làm việc: " + response.code())));
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("ShiftRepository", "refreshShiftsFromApi exception: " + e.getMessage(), e);
                if (callback != null) {
                    exec.mainThread().execute(() -> callback.onError(e));
                }
            }
        });
    }

    private void refreshShifts() {
        refreshShiftsFromApi(null);
    }

    // ── Mapping ──
    private ShiftTemplateEntity mapTemplateResponseToEntity(ShiftTemplateResponse r) {
        ShiftTemplateEntity e = new ShiftTemplateEntity();
        e.setTemplateId(r.getTemplateId());
        e.setTemplateName(r.getTemplateName());
        e.setStartTime(r.getStartTime());
        e.setEndTime(r.getEndTime());
        e.setMinStaff(r.getMinStaff());
        e.setActive(r.isActive());
        e.setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt() : System.currentTimeMillis());
        return e;
    }

    private ShiftEntity mapShiftResponseToEntity(ShiftResponse r) {
        ShiftEntity e = new ShiftEntity();
        e.setShiftId(r.getShiftId());
        e.setTemplateId(r.getTemplateId());
        e.setShiftName(r.getShiftName());
        e.setShiftDate(r.getShiftDate() != null ? r.getShiftDate() : 0);
        e.setStartTime(r.getStartTime());
        e.setEndTime(r.getEndTime());
        e.setStatus(r.getStatus());
        e.setOpenedBy(r.getOpenedBy() != null ? r.getOpenedBy() : 0);
        e.setOpenedAt(r.getOpenedAt() != null ? r.getOpenedAt() : 0);
        e.setClosedBy(r.getClosedBy() != null ? r.getClosedBy() : 0);
        e.setClosedAt(r.getClosedAt() != null ? r.getClosedAt() : 0);
        e.setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt() : 0);
        return e;
    }
}
