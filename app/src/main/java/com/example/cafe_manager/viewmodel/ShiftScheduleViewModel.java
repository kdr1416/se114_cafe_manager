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
    private final AppDatabase appDatabase;
    private final SessionManager sessionManager;

    private final MutableLiveData<Long> selectedDate = new MutableLiveData<>();
    private final LiveData<List<ShiftEntity>> rawShifts;
    private final MediatorLiveData<List<ShiftDisplayItem>> shiftsLive = new MediatorLiveData<>();
    private final LiveData<List<ShiftTemplateEntity>> templatesLive;
    private final MutableLiveData<String> statusFilter = new MutableLiveData<>("ALL");
    private final MutableLiveData<String> messageLive = new MutableLiveData<>();

    // ── Inner classes ──

    public static class ShiftDisplayItem {
        public final ShiftEntity shift;
        public final int assignedCount;
        public final int minStaff;
        public final boolean understaffed;

        public ShiftDisplayItem(ShiftEntity shift, int assignedCount, int minStaff) {
            this.shift = shift;
            this.assignedCount = assignedCount;
            this.minStaff = minStaff;
            this.understaffed = assignedCount < minStaff &&
                    !Constants.SHIFT_CLOSED.equals(shift.getStatus()) &&
                    !Constants.SHIFT_CANCELLED.equals(shift.getStatus());
        }
    }

    public static class StaffAssignmentData {
        public final List<UserEntity> allUsers;
        public final List<ShiftAssignmentEntity> currentAssignments;
        public final java.util.Map<Integer, Integer> weeklyShiftCounts;
        public StaffAssignmentData(List<UserEntity> allUsers,
                                   List<ShiftAssignmentEntity> currentAssignments,
                                   java.util.Map<Integer, Integer> weeklyShiftCounts) {
            this.allUsers = allUsers;
            this.currentAssignments = currentAssignments;
            this.weeklyShiftCounts = weeklyShiftCounts;
        }
    }

    // ── Constructor ──

    public ShiftScheduleViewModel(@NonNull Application application) {
        super(application);
        shiftRepository = ShiftRepository.getInstance(application);
        appDatabase = AppDatabase.getInstance(application);
        sessionManager = SessionManager.getInstance(application);
        templatesLive = shiftRepository.getActiveTemplates();

        // Reactive: khi selectedDate thay đổi → tự động load shifts
        rawShifts = Transformations.switchMap(selectedDate,
                date -> shiftRepository.getShiftsByDate(date));

        // Enrich và lọc mỗi shift
        shiftsLive.addSource(rawShifts, shifts -> reloadAndFilterShifts());
        shiftsLive.addSource(statusFilter, filter -> reloadAndFilterShifts());

        // Mặc định chọn hôm nay
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        selectedDate.setValue(cal.getTimeInMillis());

        // Trigger network refresh
        shiftRepository.refreshTemplatesFromApi(null);
        shiftRepository.refreshShiftsFromApi(null);
    }

    private void reloadAndFilterShifts() {
        List<ShiftEntity> shifts = rawShifts.getValue();
        String filter = statusFilter.getValue();
        if (shifts == null) {
            shiftsLive.setValue(null);
            return;
        }

        AppExecutors.getInstance().diskIO().execute(() -> {
            List<ShiftDisplayItem> items = new ArrayList<>();
            for (ShiftEntity s : shifts) {
                // Lọc theo status
                if (filter != null && !"ALL".equals(filter)) {
                    if (!filter.equals(s.getStatus())) {
                        continue;
                    }
                }

                int count = appDatabase.shiftAssignmentDao().countByShift(s.getShiftId());
                int minStaff = 0;
                if (s.getTemplateId() != null) {
                    ShiftTemplateEntity t = appDatabase.shiftTemplateDao().getById(s.getTemplateId());
                    if (t != null) {
                        minStaff = t.getMinStaff();
                    }
                }
                items.add(new ShiftDisplayItem(s, count, minStaff));
            }
            AppExecutors.getInstance().mainThread().execute(() -> shiftsLive.setValue(items));
        });
    }

    // ── Getters ──

    public LiveData<List<ShiftDisplayItem>> getShifts() { return shiftsLive; }
    public LiveData<List<ShiftTemplateEntity>> getTemplates() { return templatesLive; }
    public LiveData<String> getMessage() { return messageLive; }
    public void clearMessage() { messageLive.setValue(null); }
    public void setStatusFilter(String filter) { statusFilter.setValue(filter); }
    public String getStatusFilter() { return statusFilter.getValue(); }
    public long getSelectedDate() {
        Long d = selectedDate.getValue();
        return d != null ? d : System.currentTimeMillis();
    }

    // ── Date ──

    public void setDate(long dateMidnight) {
        selectedDate.setValue(dateMidnight);
        shiftRepository.refreshShiftsFromApi(null);
    }

    public void refresh() {
        shiftRepository.refreshShiftsFromApi(null);
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

    public void publishShift(int shiftId) {
        shiftRepository.publishShift(shiftId, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                messageLive.setValue("Đã phát hành ca.");
                refresh();
            }

            @Override
            public void onError(Exception e) {
                messageLive.setValue("Lỗi phát hành ca: " + e.getMessage());
            }
        });
    }

    public void cancelShift(int shiftId) {
        shiftRepository.cancelShift(shiftId, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                messageLive.setValue("Đã hủy ca.");
                refresh();
            }

            @Override
            public void onError(Exception e) {
                messageLive.setValue("Lỗi hủy ca: " + e.getMessage());
            }
        });
    }

    // ── Mở ca + tạo phiên két tiền mặt ──

    public void openShiftWithCash(int shiftId, double openingCash) {
        int userId = sessionManager.getUserId();
        shiftRepository.openShiftWithCash(shiftId, openingCash, userId, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                messageLive.setValue("Đã mở ca và két tiền mặt.");
                refresh();
            }
            @Override
            public void onError(Exception e) {
                messageLive.setValue("Lỗi mở ca: " + e.getMessage());
            }
        });
    }

    // ── Staff assignment ──

    /** Load dữ liệu để hiển thị dialog phân công. */
    public void loadStaffForAssignment(int shiftId, RepositoryCallback<StaffAssignmentData> callback) {
        shiftRepository.getShiftById(shiftId, new RepositoryCallback<ShiftEntity>() {
            @Override
            public void onSuccess(ShiftEntity shift) {
                AppExecutors.getInstance().diskIO().execute(() -> {
                    try {
                        long date = shift != null ? shift.getShiftDate() : System.currentTimeMillis();

                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(date);
                        cal.setFirstDayOfWeek(Calendar.MONDAY);

                        // Get Monday 00:00:00.000 of the week
                        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                        cal.set(Calendar.HOUR_OF_DAY, 0);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.SECOND, 0);
                        cal.set(Calendar.MILLISECOND, 0);
                        long weekStart = cal.getTimeInMillis();

                        // Get Sunday 23:59:59.999 of the week
                        cal.add(Calendar.DATE, 6);
                        cal.set(Calendar.HOUR_OF_DAY, 23);
                        cal.set(Calendar.MINUTE, 59);
                        cal.set(Calendar.SECOND, 59);
                        cal.set(Calendar.MILLISECOND, 999);
                        long weekEnd = cal.getTimeInMillis();

                        List<UserEntity> users = appDatabase.userDao().getActiveUsersSync();
                        List<ShiftAssignmentEntity> assigned =
                                appDatabase.shiftAssignmentDao().getByShiftSync(shiftId);

                        java.util.Map<Integer, Integer> weeklyShiftCounts = new java.util.HashMap<>();
                        for (UserEntity u : users) {
                            int count = appDatabase.shiftAssignmentDao().countAssignmentsInRange(u.getUserId(), weekStart, weekEnd);
                            weeklyShiftCounts.put(u.getUserId(), count);
                        }

                        StaffAssignmentData data = new StaffAssignmentData(users, assigned, weeklyShiftCounts);
                        AppExecutors.getInstance().mainThread().execute(() -> callback.onSuccess(data));
                    } catch (Exception e) {
                        AppExecutors.getInstance().mainThread().execute(() -> callback.onError(e));
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
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
                refresh();
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
                refresh();
            }
            @Override
            public void onError(Exception e) {
                messageLive.setValue("Lỗi: " + e.getMessage());
            }
        });
    }

    public void bulkCreateShifts(List<ShiftTemplateEntity> templates) {
        Long date = selectedDate.getValue();
        if (date == null) { messageLive.setValue("Chưa chọn ngày."); return; }

        createShiftsRecursively(templates, date, 0, 0);
    }

    private void createShiftsRecursively(List<ShiftTemplateEntity> templates, long date, int index, int successCount) {
        if (index >= templates.size()) {
            messageLive.setValue("Đã tạo " + successCount + " ca làm việc.");
            refresh();
            return;
        }

        ShiftTemplateEntity t = templates.get(index);
        ShiftEntity shift = new ShiftEntity();
        shift.setTemplateId(t.getTemplateId());
        shift.setShiftName(t.getTemplateName());
        shift.setShiftDate(date);
        shift.setStartTime(t.getStartTime());
        shift.setEndTime(t.getEndTime());
        shift.setStatus(Constants.SHIFT_DRAFT);
        shift.setCreatedAt(System.currentTimeMillis());

        shiftRepository.insertShift(shift, new RepositoryCallback<Long>() {
            @Override
            public void onSuccess(Long result) {
                createShiftsRecursively(templates, date, index + 1, successCount + 1);
            }

            @Override
            public void onError(Exception e) {
                createShiftsRecursively(templates, date, index + 1, successCount);
            }
        });
    }

    public void copySchedule(long sourceDate, long targetDate) {
        if (sourceDate == targetDate) {
            messageLive.setValue("Không thể sao chép vào cùng ngày.");
            return;
        }

        List<ShiftDisplayItem> currentItems = shiftsLive.getValue();
        if (currentItems == null || currentItems.isEmpty()) {
            messageLive.setValue("Ngày nguồn không có ca nào để sao chép.");
            return;
        }

        List<ShiftEntity> shiftsToCopy = new ArrayList<>();
        for (ShiftDisplayItem item : currentItems) {
            if (item.shift != null && !Constants.SHIFT_CANCELLED.equals(item.shift.getStatus())) {
                shiftsToCopy.add(item.shift);
            }
        }

        if (shiftsToCopy.isEmpty()) {
            messageLive.setValue("Ngày nguồn không có ca hợp lệ để sao chép.");
            return;
        }

        copyShiftsRecursively(shiftsToCopy, targetDate, 0, 0);
    }

    private void copyShiftsRecursively(List<ShiftEntity> sourceShifts, long targetDate, int index, int successCount) {
        if (index >= sourceShifts.size()) {
            messageLive.setValue("Đã sao chép " + successCount + " ca làm việc (chỉ khung ca).");
            setDate(targetDate);
            return;
        }

        ShiftEntity src = sourceShifts.get(index);
        ShiftEntity newShift = new ShiftEntity();
        newShift.setTemplateId(src.getTemplateId());
        newShift.setShiftName(src.getShiftName());
        newShift.setShiftDate(targetDate);
        newShift.setStartTime(src.getStartTime());
        newShift.setEndTime(src.getEndTime());
        newShift.setStatus(Constants.SHIFT_DRAFT);
        newShift.setCreatedAt(System.currentTimeMillis());

        shiftRepository.insertShift(newShift, new RepositoryCallback<Long>() {
            @Override
            public void onSuccess(Long result) {
                copyShiftsRecursively(sourceShifts, targetDate, index + 1, successCount + 1);
            }

            @Override
            public void onError(Exception e) {
                copyShiftsRecursively(sourceShifts, targetDate, index + 1, successCount);
            }
        });
    }

    public void getOrCreateShiftChatRoom(int shiftId, RepositoryCallback<Integer> callback) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                com.example.cafe_manager.data.repository.ChatRepository.syncShiftChatRoomSync(appDatabase, shiftId);
                com.example.cafe_manager.data.local.entity.ChatRoomEntity room = appDatabase.chatRoomDao().getByShiftId(shiftId);
                if (room != null) {
                    AppExecutors.getInstance().mainThread().execute(() -> callback.onSuccess(room.getRoomId()));
                } else {
                    AppExecutors.getInstance().mainThread().execute(() -> callback.onError(new Exception("Không thể tạo phòng chat cho ca này.")));
                }
            } catch (Exception e) {
                AppExecutors.getInstance().mainThread().execute(() -> callback.onError(e));
            }
        });
    }
}