package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.entity.ShiftAssignmentEntity;
import com.example.cafe_manager.data.local.entity.ShiftEntity;
import com.example.cafe_manager.data.local.entity.ShiftTemplateEntity;
import com.example.cafe_manager.data.local.entity.UserEntity;
import com.example.cafe_manager.data.repository.ShiftReportRepository;
import com.example.cafe_manager.data.repository.ShiftRepository;
import com.example.cafe_manager.manager.SessionManager;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.RepositoryCallback;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ShiftScheduleViewModel extends AndroidViewModel {

    private final ShiftRepository shiftRepository;
    private final ShiftReportRepository reportRepository;
    private final AppDatabase appDatabase;
    private final SessionManager sessionManager;

    private final MutableLiveData<Long> selectedDate = new MutableLiveData<>();
    private final LiveData<List<ShiftEntity>> rawShifts;
    private final MediatorLiveData<List<ShiftDisplayItem>> shiftsLive = new MediatorLiveData<>();
    private final LiveData<List<ShiftTemplateEntity>> templatesLive;
    private final MutableLiveData<String> messageLive = new MutableLiveData<>();

    // ── Inner classes ──

    public static class ShiftDisplayItem {
        public final ShiftEntity shift;
        public final int assignedCount;
        public ShiftDisplayItem(ShiftEntity shift, int assignedCount) {
            this.shift = shift;
            this.assignedCount = assignedCount;
        }
    }

    public static class StaffAssignmentData {
        public final List<UserEntity> allUsers;
        public final List<ShiftAssignmentEntity> currentAssignments;
        public StaffAssignmentData(List<UserEntity> allUsers,
                                   List<ShiftAssignmentEntity> currentAssignments) {
            this.allUsers = allUsers;
            this.currentAssignments = currentAssignments;
        }
    }

    // ── Constructor ──

    public ShiftScheduleViewModel(@NonNull Application application) {
        super(application);
        shiftRepository = new ShiftRepository(application);
        reportRepository = new ShiftReportRepository(application);
        appDatabase = AppDatabase.getInstance(application);
        sessionManager = SessionManager.getInstance(application);
        templatesLive = shiftRepository.getActiveTemplates();

        // Reactive: khi selectedDate thay đổi → tự động load shifts
        rawShifts = Transformations.switchMap(selectedDate,
                date -> shiftRepository.getShiftsByDate(date));

        // Enrich mỗi shift với assignedCount
        shiftsLive.addSource(rawShifts, shifts -> {
            if (shifts == null) { shiftsLive.setValue(null); return; }
            AppExecutors.getInstance().diskIO().execute(() -> {
                List<ShiftDisplayItem> items = new ArrayList<>();
                for (ShiftEntity s : shifts) {
                    int count = appDatabase.shiftAssignmentDao().countByShift(s.getShiftId());
                    items.add(new ShiftDisplayItem(s, count));
                }
                AppExecutors.getInstance().mainThread().execute(() -> shiftsLive.setValue(items));
            });
        });

        // Mặc định chọn hôm nay
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        selectedDate.setValue(cal.getTimeInMillis());
    }

    // ── Getters ──

    public LiveData<List<ShiftDisplayItem>> getShifts() { return shiftsLive; }
    public LiveData<List<ShiftTemplateEntity>> getTemplates() { return templatesLive; }
    public LiveData<String> getMessage() { return messageLive; }
    public void clearMessage() { messageLive.setValue(null); }
    public long getSelectedDate() {
        Long d = selectedDate.getValue();
        return d != null ? d : System.currentTimeMillis();
    }

    // ── Date ──

    public void setDate(long dateMidnight) {
        selectedDate.setValue(dateMidnight);
    }

    // ── Tạo ca từ template ──

    public void createShiftFromTemplate(ShiftTemplateEntity template) {
        Long date = selectedDate.getValue();
        if (date == null) { messageLive.setValue("Chưa chọn ngày."); return; }

        ShiftEntity shift = new ShiftEntity();
        shift.setTemplateId(template.getTemplateId());
        shift.setShiftName(template.getTemplateName());
        shift.setShiftDate(date);
        shift.setStartTime(template.getStartTime());
        shift.setEndTime(template.getEndTime());
        shift.setStatus(Constants.SHIFT_DRAFT);
        shift.setCreatedAt(System.currentTimeMillis());

        shiftRepository.insertShift(shift, new RepositoryCallback<Long>() {
            @Override
            public void onSuccess(Long result) {
                messageLive.setValue("Đã tạo ca \"" + template.getTemplateName() + "\".");
            }
            @Override
            public void onError(Exception e) {
                messageLive.setValue("Lỗi: " + e.getMessage());
            }
        });
    }

    // ── Publish / Cancel ──

    public void publishShift(int shiftId) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                appDatabase.shiftDao().updateStatus(shiftId, Constants.SHIFT_PUBLISHED);
                AppExecutors.getInstance().mainThread().execute(
                        () -> messageLive.setValue("Đã phát hành ca."));
            } catch (Exception e) {
                AppExecutors.getInstance().mainThread().execute(
                        () -> messageLive.setValue("Lỗi: " + e.getMessage()));
            }
        });
    }

    public void cancelShift(int shiftId) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                appDatabase.shiftDao().updateStatus(shiftId, Constants.SHIFT_CANCELLED);
                AppExecutors.getInstance().mainThread().execute(
                        () -> messageLive.setValue("Đã hủy ca."));
            } catch (Exception e) {
                AppExecutors.getInstance().mainThread().execute(
                        () -> messageLive.setValue("Lỗi: " + e.getMessage()));
            }
        });
    }

    // ── Mở ca + tạo phiên két tiền mặt ──

    public void openShiftWithCash(int shiftId, double openingCash) {
        int userId = sessionManager.getUserId();
        shiftRepository.openShift(shiftId, userId, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // Ca đã mở → tạo phiên két tiền mặt
                reportRepository.openCashSession(shiftId, openingCash, userId,
                        new RepositoryCallback<Long>() {
                            @Override
                            public void onSuccess(Long sessionId) {
                                messageLive.setValue("Đã mở ca và két tiền mặt.");
                            }
                            @Override
                            public void onError(Exception e) {
                                messageLive.setValue("Ca đã mở, nhưng lỗi két: " + e.getMessage());
                            }
                        });
            }
            @Override
            public void onError(Exception e) {
                messageLive.setValue(e.getMessage());
            }
        });
    }

    // ── Staff assignment ──

    /** Load dữ liệu để hiển thị dialog phân công. */
    public void loadStaffForAssignment(int shiftId, RepositoryCallback<StaffAssignmentData> callback) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                List<UserEntity> users = appDatabase.userDao().getActiveUsersSync();
                List<ShiftAssignmentEntity> assigned =
                        appDatabase.shiftAssignmentDao().getByShiftSync(shiftId);
                StaffAssignmentData data = new StaffAssignmentData(users, assigned);
                AppExecutors.getInstance().mainThread().execute(() -> callback.onSuccess(data));
            } catch (Exception e) {
                AppExecutors.getInstance().mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void assignStaff(int shiftId, int userId) {
        ShiftAssignmentEntity assignment = new ShiftAssignmentEntity();
        assignment.setShiftId(shiftId);
        assignment.setUserId(userId);
        assignment.setRole(Constants.ROLE_STAFF);
        assignment.setAssignedBy(sessionManager.getUserId());
        assignment.setConfirmed(false);
        assignment.setCreatedAt(System.currentTimeMillis());

        shiftRepository.assignStaff(assignment, new RepositoryCallback<Long>() {
            @Override
            public void onSuccess(Long result) {
                messageLive.setValue("Đã phân công nhân viên.");
            }
            @Override
            public void onError(Exception e) {
                messageLive.setValue(e.getMessage());
            }
        });
    }

    public void removeAssignment(int assignmentId) {
        shiftRepository.removeAssignment(assignmentId, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                messageLive.setValue("Đã hủy phân công.");
            }
            @Override
            public void onError(Exception e) {
                messageLive.setValue("Lỗi: " + e.getMessage());
            }
        });
    }
}