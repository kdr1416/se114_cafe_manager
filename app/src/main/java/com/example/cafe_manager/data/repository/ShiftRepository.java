package com.example.cafe_manager.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.dao.ShiftAssignmentDao;
import com.example.cafe_manager.data.local.dao.ShiftDao;
import com.example.cafe_manager.data.local.dao.ShiftTemplateDao;
import com.example.cafe_manager.data.local.dao.ShiftTransactionDao;
import com.example.cafe_manager.data.local.entity.ShiftAssignmentEntity;
import com.example.cafe_manager.data.local.entity.ShiftEntity;
import com.example.cafe_manager.data.local.entity.ShiftTemplateEntity;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.RepositoryCallback;
import com.example.cafe_manager.util.ShiftTimeUtils;

import java.util.List;

public class ShiftRepository {
    private final ShiftTemplateDao templateDao;
    private final ShiftDao shiftDao;
    private final ShiftAssignmentDao assignmentDao;
    private final ShiftTransactionDao transactionDao;
    private final AppExecutors executors;

    public ShiftRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        templateDao = db.shiftTemplateDao();
        shiftDao = db.shiftDao();
        assignmentDao = db.shiftAssignmentDao();
        transactionDao = db.shiftTransactionDao();
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

    public void openShiftWithCash(int shiftId, double openingCash, int userId, RepositoryCallback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
<<<<<<< HEAD
                transactionDao.openShiftWithCashAtomic(shiftId, openingCash, userId, System.currentTimeMillis());
=======
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
>>>>>>> 133595b62c24b13c0d3d38cbe4582385da7ddfea
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
                // Lấy ca làm việc để kiểm tra
                ShiftEntity shift = shiftDao.getById(assignment.getShiftId());
                if (shift == null) {
                    executors.mainThread().execute(() -> callback.onError(new Exception("Ca làm việc không tồn tại.")));
                    return;
                }

                // Chặn phân công khi ca đang mở/đóng/hủy
                if (Constants.SHIFT_IN_PROGRESS.equals(shift.getStatus()) || 
                    Constants.SHIFT_CLOSED.equals(shift.getStatus()) || 
                    Constants.SHIFT_CANCELLED.equals(shift.getStatus())) {
                    executors.mainThread().execute(() -> callback.onError(new Exception("Không thể phân công nhân viên cho ca đang chạy, đã đóng hoặc đã hủy.")));
                    return;
                }
                
                // Kiểm tra overlap xuyên ngày sử dụng ShiftTimeUtils
                List<ShiftAssignmentEntity> userAssignments = assignmentDao.getByUserSync(assignment.getUserId());
                for (ShiftAssignmentEntity existingAssign : userAssignments) {
                    if (existingAssign.getShiftId() == assignment.getShiftId()) {
                        continue;
                    }
                    ShiftEntity existingShift = shiftDao.getById(existingAssign.getShiftId());
                    if (existingShift != null && !Constants.SHIFT_CANCELLED.equals(existingShift.getStatus())) {
                        if (ShiftTimeUtils.checkOverlap(shift.getShiftDate(), shift.getStartTime(), shift.getEndTime(),
                                     existingShift.getShiftDate(), existingShift.getStartTime(), existingShift.getEndTime())) {
                            executors.mainThread().execute(() -> callback.onError(new Exception("Nhân viên này đã bị trùng ca làm việc khác (" + existingShift.getShiftName() + ").")));
                            return;
                        }
                    }
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
                String shiftStatus = assignmentDao.getShiftStatusByAssignmentId(assignmentId);
                if (shiftStatus != null) {
                    if (Constants.SHIFT_IN_PROGRESS.equals(shiftStatus) || 
                        Constants.SHIFT_CLOSED.equals(shiftStatus) || 
                        Constants.SHIFT_CANCELLED.equals(shiftStatus)) {
                        executors.mainThread().execute(() -> callback.onError(new Exception("Không thể hủy phân công cho ca đang chạy, đã đóng hoặc đã hủy.")));
                        return;
                    }
                }

                assignmentDao.delete(assignmentId);
                executors.mainThread().execute(() -> callback.onSuccess(null));
            } catch (Exception e) { executors.mainThread().execute(() -> callback.onError(e)); }
        });
    }

    public void confirmAssignment(int assignmentId, RepositoryCallback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                String shiftStatus = assignmentDao.getShiftStatusByAssignmentId(assignmentId);
                if (shiftStatus != null) {
                    if (Constants.SHIFT_CLOSED.equals(shiftStatus) || Constants.SHIFT_CANCELLED.equals(shiftStatus)) {
                        executors.mainThread().execute(() -> callback.onError(new Exception("Không thể xác nhận phân công cho ca đã đóng hoặc đã hủy.")));
                        return;
                    }
                }
                assignmentDao.confirm(assignmentId);
                executors.mainThread().execute(() -> callback.onSuccess(null));
            } catch (Exception e) { executors.mainThread().execute(() -> callback.onError(e)); }
        });
    }
}
