package com.example.cafe_manager.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.dao.ShiftAssignmentDao;
import com.example.cafe_manager.data.local.dao.ShiftDao;
import com.example.cafe_manager.data.local.dao.ShiftTemplateDao;
import com.example.cafe_manager.data.local.entity.ShiftAssignmentEntity;
import com.example.cafe_manager.data.local.entity.ShiftEntity;
import com.example.cafe_manager.data.local.entity.ShiftTemplateEntity;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.RepositoryCallback;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ShiftRepository {
    private final ShiftTemplateDao templateDao;
    private final ShiftDao shiftDao;
    private final ShiftAssignmentDao assignmentDao;
    private final AppExecutors executors;

    public ShiftRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        templateDao = db.shiftTemplateDao();
        shiftDao = db.shiftDao();
        assignmentDao = db.shiftAssignmentDao();
        executors = AppExecutors.getInstance();
    }

    // ── Template ──
    public LiveData<List<ShiftTemplateEntity>> getAllTemplates() { return templateDao.getAll(); }
    public LiveData<List<ShiftTemplateEntity>> getActiveTemplates() { return templateDao.getActive(); }

    public void insertTemplate(ShiftTemplateEntity template, RepositoryCallback<Long> callback) {
        executors.diskIO().execute(() -> {
            try {
                long id = templateDao.insert(template);
                executors.mainThread().execute(() -> callback.onSuccess(id));
            } catch (Exception e) { executors.mainThread().execute(() -> callback.onError(e)); }
        });
    }

    public void updateTemplate(ShiftTemplateEntity template, RepositoryCallback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                templateDao.update(template);
                executors.mainThread().execute(() -> callback.onSuccess(null));
            } catch (Exception e) { executors.mainThread().execute(() -> callback.onError(e)); }
        });
    }

    public void deactivateTemplate(int templateId, RepositoryCallback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                templateDao.deactivate(templateId);
                executors.mainThread().execute(() -> callback.onSuccess(null));
            } catch (Exception e) { executors.mainThread().execute(() -> callback.onError(e)); }
        });
    }

    // ── Shift ──
    public LiveData<List<ShiftEntity>> getShiftsByDate(long date) { return shiftDao.getByDate(date); }

    public void insertShift(ShiftEntity shift, RepositoryCallback<Long> callback) {
        executors.diskIO().execute(() -> {
            try {
                long id = shiftDao.insert(shift);
                executors.mainThread().execute(() -> callback.onSuccess(id));
            } catch (Exception e) { executors.mainThread().execute(() -> callback.onError(e)); }
        });
    }

    public void openShift(int shiftId, int userId, RepositoryCallback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                ShiftEntity openShift = shiftDao.getCurrentlyOpen();
                if (openShift != null) {
                    if (openShift.getShiftId() == shiftId) {
                        // If this specific shift is already open, allow proceeding (idempotency)
                        executors.mainThread().execute(() -> callback.onSuccess(null));
                    } else {
                        // Provide a clear error message about which shift is blocking
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        String dateStr = sdf.format(new Date(openShift.getShiftDate()));
                        String errorMsg = "Ca '" + openShift.getShiftName() + "' ngày " + dateStr + " đang mở. Hãy đóng ca đó trước.";
                        executors.mainThread().execute(() -> callback.onError(new Exception(errorMsg)));
                    }
                    return;
                }
                shiftDao.openShift(shiftId, userId, System.currentTimeMillis());
                executors.mainThread().execute(() -> callback.onSuccess(null));
            } catch (Exception e) {
                executors.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    // ── Assignment ──
    public void assignStaff(ShiftAssignmentEntity assignment, RepositoryCallback<Long> callback) {
        executors.diskIO().execute(() -> {
            try {
                long id = assignmentDao.insert(assignment);
                executors.mainThread().execute(() -> callback.onSuccess(id));
            } catch (Exception e) { executors.mainThread().execute(() -> callback.onError(e)); }
        });
    }

    public void removeAssignment(int assignmentId, RepositoryCallback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                assignmentDao.delete(assignmentId);
                executors.mainThread().execute(() -> callback.onSuccess(null));
            } catch (Exception e) { executors.mainThread().execute(() -> callback.onError(e)); }
        });
    }

    public void confirmAssignment(int assignmentId, RepositoryCallback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                assignmentDao.confirm(assignmentId);
                executors.mainThread().execute(() -> callback.onSuccess(null));
            } catch (Exception e) { executors.mainThread().execute(() -> callback.onError(e)); }
        });
    }
}
