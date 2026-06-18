package com.example.cafe_manager.data.repository;

import android.content.Context;

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

import java.util.List;

public class ShiftRepository {

    private final ShiftTemplateDao templateDao;
    private final ShiftDao shiftDao;
    private final ShiftAssignmentDao assignmentDao;
    private final AppExecutors executors;

    public ShiftRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.templateDao = db.shiftTemplateDao();
        this.shiftDao = db.shiftDao();
        this.assignmentDao = db.shiftAssignmentDao();
        this.executors = AppExecutors.getInstance();
    }

    // ── Template Operations ──────────────────────────────────────────

    public LiveData<List<ShiftTemplateEntity>> getAllTemplates() {
        return templateDao.getAll();
    }

    public LiveData<List<ShiftTemplateEntity>> getActiveTemplates() {
        return templateDao.getActive();
    }

    public void insertTemplate(ShiftTemplateEntity template, RepositoryCallback<Long> callback) {
        executors.diskIO().execute(() -> {
            try {
                long id = templateDao.insert(template);
                executors.mainThread().execute(() -> callback.onSuccess(id));
            } catch (Exception e) {
                executors.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void updateTemplate(ShiftTemplateEntity template, RepositoryCallback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                templateDao.update(template);
                executors.mainThread().execute(() -> callback.onSuccess(null));
            } catch (Exception e) {
                executors.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void deactivateTemplate(int templateId, RepositoryCallback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                templateDao.deactivate(templateId);
                executors.mainThread().execute(() -> callback.onSuccess(null));
            } catch (Exception e) {
                executors.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    // ── Shift Operations ──────────────────────────────────────────────

    public LiveData<List<ShiftEntity>> getShiftsByDate(long dateMidnight) {
        return shiftDao.getByDate(dateMidnight);
    }

    public void insertShift(ShiftEntity shift, RepositoryCallback<Long> callback) {
        executors.diskIO().execute(() -> {
            try {
                long id = shiftDao.insert(shift);
                executors.mainThread().execute(() -> callback.onSuccess(id));
            } catch (Exception e) {
                executors.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void updateShift(ShiftEntity shift, RepositoryCallback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                shiftDao.update(shift);
                executors.mainThread().execute(() -> callback.onSuccess(null));
            } catch (Exception e) {
                executors.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void openShift(int id, int openedBy, RepositoryCallback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                ShiftEntity openShift = shiftDao.getCurrentlyOpen();
                if (openShift != null) {
                    executors.mainThread().execute(() -> callback.onError(new Exception("Đã có ca làm việc khác đang mở.")));
                    return;
                }
                shiftDao.openShift(id, openedBy, System.currentTimeMillis());
                executors.mainThread().execute(() -> callback.onSuccess(null));
            } catch (Exception e) {
                executors.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void closeShift(int id, int closedBy, RepositoryCallback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                shiftDao.closeShift(id, closedBy, System.currentTimeMillis());
                executors.mainThread().execute(() -> callback.onSuccess(null));
            } catch (Exception e) {
                executors.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    // ── Assignment Operations ────────────────────────────────────────

    public LiveData<List<ShiftAssignmentEntity>> getAssignmentsByShift(int shiftId) {
        return assignmentDao.getByShift(shiftId);
    }

    public LiveData<List<ShiftAssignmentEntity>> getAssignmentsByUser(int userId) {
        return assignmentDao.getByUser(userId);
    }

    public void assignStaff(ShiftAssignmentEntity assignment, RepositoryCallback<Long> callback) {
        executors.diskIO().execute(() -> {
            try {
                // Lấy ca làm việc để kiểm tra overlap
                ShiftEntity shift = shiftDao.getById(assignment.getShiftId());
                if (shift == null) {
                    executors.mainThread().execute(() -> callback.onError(new Exception("Ca làm việc không tồn tại.")));
                    return;
                }
                
                // Kiểm tra overlap
                List<ShiftAssignmentEntity> overlap = assignmentDao.getOverlapping(
                        assignment.getUserId(),
                        shift.getShiftDate(),
                        shift.getStartTime(),
                        shift.getEndTime()
                );
                
                if (overlap != null && !overlap.isEmpty()) {
                    executors.mainThread().execute(() -> callback.onError(new Exception("Nhân viên này đã bị trùng ca làm việc khác trong ngày.")));
                    return;
                }

                long id = assignmentDao.insert(assignment);
                executors.mainThread().execute(() -> callback.onSuccess(id));
            } catch (Exception e) {
                executors.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void removeAssignment(int assignmentId, RepositoryCallback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                assignmentDao.delete(assignmentId);
                executors.mainThread().execute(() -> callback.onSuccess(null));
            } catch (Exception e) {
                executors.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void confirmAssignment(int assignmentId, RepositoryCallback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                assignmentDao.confirm(assignmentId);
                executors.mainThread().execute(() -> callback.onSuccess(null));
            } catch (Exception e) {
                executors.mainThread().execute(() -> callback.onError(e));
            }
        });
    }
}
