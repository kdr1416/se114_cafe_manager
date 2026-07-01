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
import com.example.cafe_manager.data.repository.AuthRepository;
import com.example.cafe_manager.data.remote.ShiftAssignmentResponse;
import com.example.cafe_manager.data.remote.ShiftWithAssignmentsResponse;
import com.example.cafe_manager.data.remote.CreateShiftRequest;
import com.example.cafe_manager.data.remote.ShiftResponse;
import com.example.cafe_manager.data.repository.ChatRepository;
import com.example.cafe_manager.manager.SessionManager;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.RepositoryCallback;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import com.example.cafe_manager.data.repository.AvailabilityRepository;
import com.example.cafe_manager.data.remote.UserResponse;

public class ShiftScheduleViewModel extends AndroidViewModel {

    private final ShiftRepository shiftRepository;
    private final AppDatabase appDatabase;
    private final SessionManager sessionManager;

    private final MutableLiveData<Long> selectedDate = new MutableLiveData<>();
    private final LiveData<List<ShiftEntity>> rawShifts;
    private final LiveData<List<ShiftDisplayItem>> shiftsLive;
    private final LiveData<List<ShiftTemplateEntity>> templatesLive;
    private final MutableLiveData<String> statusFilter = new MutableLiveData<>("ALL");
    private final MutableLiveData<String> messageLive = new MutableLiveData<>();
    private final MutableLiveData<List<ShiftDisplayItem>> weekShiftsLive = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    // ── Inner classes ──

    public interface DisplayItem {
        int TYPE_HEADER = 0;
        int TYPE_SHIFT = 1;
        int getViewType();
    }

    public static class DayHeaderItem implements DisplayItem {
        public final String dayLabel;
        public final long date;
        public DayHeaderItem(String dayLabel, long date) {
            this.dayLabel = dayLabel;
            this.date = date;
        }
        @Override public int getViewType() { return TYPE_HEADER; }
    }

    public static class ShiftDisplayItem implements DisplayItem {
        public final ShiftEntity shift;
        public final int assignedCount;
        public final int minStaff;
        public final List<String> assignedStaffNames;
        public final boolean understaffed;

        public ShiftDisplayItem(ShiftEntity shift, int assignedCount, int minStaff, List<String> staffNames) {
            this.shift = shift;
            this.assignedCount = assignedCount;
            this.minStaff = minStaff;
            this.assignedStaffNames = staffNames != null ? staffNames : new ArrayList<>();
            this.understaffed = assignedCount < minStaff &&
                    !Constants.SHIFT_CLOSED.equals(shift.getStatus()) &&
                    !Constants.SHIFT_CANCELLED.equals(shift.getStatus());
        }

        @Override public int getViewType() { return TYPE_SHIFT; }
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

        // shiftsLive đơn giản chỉ nạp danh sách ca cho selectedDate mà không cần nạp phân công
        shiftsLive = Transformations.map(rawShifts, shifts -> {
            List<ShiftDisplayItem> items = new ArrayList<>();
            if (shifts != null) {
                for (ShiftEntity s : shifts) {
                    items.add(new ShiftDisplayItem(s, 0, 0, new ArrayList<>()));
                }
            }
            return items;
        });

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



    // ── Getters ──

    public LiveData<List<ShiftDisplayItem>> getShifts() { return shiftsLive; }
    public LiveData<List<ShiftDisplayItem>> getWeekShiftsLive() { return weekShiftsLive; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<List<ShiftTemplateEntity>> getTemplates() { return templatesLive; }
    public LiveData<String> getMessage() { return messageLive; }
    public void clearMessage() { messageLive.setValue(null); }
    public void setStatusFilter(String filter) { statusFilter.setValue(filter); }
    public String getStatusFilter() { return statusFilter.getValue(); }
    public long getSelectedDate() {
        Long d = selectedDate.getValue();
        return d != null ? d : System.currentTimeMillis();
    }

    public void setDate(long dateMidnight) {
        selectedDate.setValue(dateMidnight);
        loadShiftsForWeek(dateMidnight);
    }

    public void refresh() {
        loadShiftsForWeek(getSelectedDate());
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
                if (shift == null) {
                    callback.onError(new Exception("Không tìm thấy thông tin ca làm việc."));
                    return;
                }
                shiftRepository.getAssignmentsForShift(shiftId, new RepositoryCallback<List<ShiftAssignmentResponse>>() {
                    @Override
                    public void onSuccess(List<ShiftAssignmentResponse> apiAssignments) {
                        AuthRepository.getInstance(getApplication()).getActiveUsers(new RepositoryCallback<List<UserEntity>>() {
                            @Override
                            public void onSuccess(List<UserEntity> users) {
                                List<UserEntity> allUsers = new ArrayList<>(users);
                                if ("MANAGER".equals(sessionManager.getRole())) {
                                    boolean selfInList = false;
                                    for (UserEntity u : allUsers) {
                                        if (u.getUserId() == sessionManager.getUserId()) {
                                            selfInList = true;
                                            break;
                                        }
                                    }
                                    if (!selfInList) {
                                        UserEntity self = new UserEntity();
                                        self.setUserId(sessionManager.getUserId());
                                        self.setUsername(sessionManager.getUsername());
                                        self.setFullName(sessionManager.getFullName());
                                        self.setRole(sessionManager.getRole());
                                        self.setActive(true);
                                        allUsers.add(self);
                                    }
                                }

                                int templateId = shift.getTemplateId() != null ? shift.getTemplateId() : 0;
                                long shiftDate = shift.getShiftDate();
                                Calendar cal = Calendar.getInstance();
                                cal.setTimeInMillis(shiftDate);
                                int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // 1=Sun...7=Sat
                                int isoDayOfWeek = (dayOfWeek == Calendar.SUNDAY) ? 7 : dayOfWeek - 1;

                                AvailabilityRepository.getInstance(getApplication()).getAvailableStaffForShift(
                                        templateId, isoDayOfWeek, shiftDate, new RepositoryCallback<List<UserResponse>>() {
                                            @Override
                                            public void onSuccess(List<UserResponse> availableUsers) {
                                                proceedWithFiltering(allUsers, availableUsers, apiAssignments, shift, callback);
                                            }

                                            @Override
                                            public void onError(Exception e) {
                                                // Fallback on error: proceed with empty available users
                                                proceedWithFiltering(allUsers, new ArrayList<>(), apiAssignments, shift, callback);
                                            }
                                        });
                            }

                            @Override
                            public void onError(Exception e) {
                                callback.onError(e);
                            }
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        callback.onError(e);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    private void proceedWithFiltering(List<UserEntity> users,
                                      List<UserResponse> availableUsers,
                                      List<ShiftAssignmentResponse> apiAssignments,
                                      ShiftEntity shift,
                                      RepositoryCallback<StaffAssignmentData> callback) {
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

                List<ShiftAssignmentEntity> assigned = new ArrayList<>();
                Set<Integer> assignedUserIds = new HashSet<>();
                if (apiAssignments != null) {
                    for (ShiftAssignmentResponse r : apiAssignments) {
                        ShiftAssignmentEntity entity = new ShiftAssignmentEntity();
                        entity.setAssignmentId(r.getAssignmentId());
                        entity.setShiftId(r.getShiftId());
                        entity.setUserId(r.getUserId());
                        entity.setRole(r.getRole());
                        entity.setConfirmed(Boolean.TRUE.equals(r.getConfirmed()));
                        entity.setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt() : 0);
                        assigned.add(entity);
                        assignedUserIds.add(r.getUserId());
                    }
                }

                Set<Integer> availableUserIds = new HashSet<>();
                if (availableUsers != null) {
                    for (UserResponse uResp : availableUsers) {
                        availableUserIds.add(uResp.getUserId());
                    }
                }

                List<UserEntity> filteredUsers = new ArrayList<>();
                for (UserEntity u : users) {
                    String role = u.getRole();
                    if ("ADMIN".equals(role)) {
                        if (assignedUserIds.contains(u.getUserId())) {
                            filteredUsers.add(u); // allow unassign only
                        }
                    } else {
                        // STAFF and MANAGER: show only if available OR already assigned
                        if (availableUserIds.contains(u.getUserId()) || assignedUserIds.contains(u.getUserId())) {
                            filteredUsers.add(u);
                        }
                    }
                }

                // TODO: weeklyShiftCounts still reads from Room DB (not synced from API).
                // This affects "shifts this week" count in assignment dialog only.
                // Known limitation — fix in future if needed.
                java.util.Map<Integer, Integer> weeklyShiftCounts = new java.util.HashMap<>();
                for (UserEntity u : filteredUsers) {
                    int count = appDatabase.shiftAssignmentDao().countAssignmentsInRange(u.getUserId(), weekStart, weekEnd);
                    weeklyShiftCounts.put(u.getUserId(), count);
                }

                StaffAssignmentData data = new StaffAssignmentData(filteredUsers, assigned, weeklyShiftCounts);
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

        List<CreateShiftRequest> requests = new ArrayList<>();
        for (ShiftTemplateEntity t : templates) {
            CreateShiftRequest req = new CreateShiftRequest();
            req.setTemplateId(t.getTemplateId());
            req.setShiftName(t.getTemplateName());
            req.setShiftDate(date);
            req.setStartTime(t.getStartTime());
            req.setEndTime(t.getEndTime());
            requests.add(req);
        }

        isLoading.setValue(true);
        shiftRepository.createShiftsBulk(requests, new RepositoryCallback<List<ShiftResponse>>() {
            @Override
            public void onSuccess(List<ShiftResponse> result) {
                isLoading.setValue(false);
                messageLive.setValue("Đã tạo " + result.size() + " ca làm việc.");
                loadShiftsForWeek(getSelectedDate());
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                messageLive.setValue("Lỗi tạo hàng loạt ca: " + e.getMessage());
            }
        });
    }

    public void copySchedule(long sourceDate, long targetDate) {
        if (sourceDate == targetDate) {
            messageLive.setValue("Không thể sao chép vào cùng ngày.");
            return;
        }

        List<ShiftDisplayItem> currentItems = weekShiftsLive.getValue();
        if (currentItems == null || currentItems.isEmpty()) {
            messageLive.setValue("Ngày nguồn không có ca nào để sao chép.");
            return;
        }

        List<CreateShiftRequest> requests = new ArrayList<>();
        Calendar calTarget = Calendar.getInstance();
        calTarget.setTimeInMillis(sourceDate);
        int targetYear = calTarget.get(Calendar.YEAR);
        int targetMonth = calTarget.get(Calendar.MONTH);
        int targetDay = calTarget.get(Calendar.DAY_OF_MONTH);

        Calendar calShift = Calendar.getInstance();

        for (ShiftDisplayItem item : currentItems) {
            if (item.shift != null && !Constants.SHIFT_CANCELLED.equals(item.shift.getStatus())) {
                calShift.setTimeInMillis(item.shift.getShiftDate());
                boolean sameDay = calShift.get(Calendar.YEAR) == targetYear &&
                                  calShift.get(Calendar.MONTH) == targetMonth &&
                                  calShift.get(Calendar.DAY_OF_MONTH) == targetDay;
                if (sameDay) {
                    CreateShiftRequest req = new CreateShiftRequest();
                    req.setTemplateId(item.shift.getTemplateId());
                    req.setShiftName(item.shift.getShiftName());
                    req.setShiftDate(targetDate);
                    req.setStartTime(item.shift.getStartTime());
                    req.setEndTime(item.shift.getEndTime());
                    requests.add(req);
                }
            }
        }

        if (requests.isEmpty()) {
            messageLive.setValue("Ngày nguồn không có ca hợp lệ để sao chép.");
            return;
        }

        isLoading.setValue(true);
        shiftRepository.createShiftsBulk(requests, new RepositoryCallback<List<ShiftResponse>>() {
            @Override
            public void onSuccess(List<ShiftResponse> result) {
                isLoading.setValue(false);
                selectedDate.setValue(targetDate);
                messageLive.setValue("Đã sao chép " + result.size() + " ca làm việc (chỉ khung ca).");
                loadShiftsForWeek(targetDate);
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                messageLive.setValue("Lỗi sao chép lịch: " + e.getMessage());
            }
        });
    }

    public void getOrCreateShiftChatRoom(int shiftId, RepositoryCallback<Integer> callback) {
        ChatRepository.getInstance(getApplication()).syncShiftRoom(shiftId, () -> {
            List<com.example.cafe_manager.data.local.entity.ChatRoomEntity> rooms = ChatRepository.getInstance(getApplication()).getActiveRooms().getValue();
            if (rooms != null) {
                for (com.example.cafe_manager.data.local.entity.ChatRoomEntity room : rooms) {
                    if (room.getShiftId() != null && room.getShiftId() == shiftId) {
                        callback.onSuccess(room.getRoomId());
                        return;
                    }
                }
            }
            ChatRepository.getInstance(getApplication()).refreshRoomsFromApi(() -> {
                List<com.example.cafe_manager.data.local.entity.ChatRoomEntity> updatedRooms = ChatRepository.getInstance(getApplication()).getActiveRooms().getValue();
                if (updatedRooms != null) {
                    for (com.example.cafe_manager.data.local.entity.ChatRoomEntity room : updatedRooms) {
                        if (room.getShiftId() != null && room.getShiftId() == shiftId) {
                            callback.onSuccess(room.getRoomId());
                            return;
                        }
                    }
                }
                callback.onError(new Exception("Không tìm thấy phòng chat cho ca này trên server."));
            });
        }, () -> {
            callback.onError(new Exception("Lỗi đồng bộ phòng chat ca làm việc với server."));
        });
    }

    public void loadShiftsForWeek(long weekStart) {
        isLoading.setValue(true);
        long normalizedWeekStart = com.example.cafe_manager.util.WeekNavigationHelper.getWeekStart(weekStart);
        shiftRepository.getShiftsForWeek(normalizedWeekStart, new RepositoryCallback<List<ShiftWithAssignmentsResponse>>() {
            @Override
            public void onSuccess(List<ShiftWithAssignmentsResponse> result) {
                isLoading.setValue(false);
                if (result == null) {
                    weekShiftsLive.setValue(new ArrayList<>());
                    return;
                }

                String filter = statusFilter.getValue();
                List<ShiftDisplayItem> items = new ArrayList<>();
                for (ShiftWithAssignmentsResponse r : result) {
                    if (filter != null && !"ALL".equals(filter) && !filter.equals(r.getStatus())) {
                        continue;
                    }

                    ShiftEntity entity = new ShiftEntity();
                    entity.setShiftId(r.getShiftId());
                    entity.setShiftName(r.getShiftName());
                    entity.setShiftDate(r.getShiftDate());
                    entity.setStartTime(r.getStartTime());
                    entity.setEndTime(r.getEndTime());
                    entity.setStatus(r.getStatus());
                    entity.setTemplateId(r.getTemplateId());

                    List<String> names = new ArrayList<>();
                    if (r.getAssignedStaff() != null) {
                        for (ShiftWithAssignmentsResponse.AssignedStaffDto s : r.getAssignedStaff()) {
                            if (s.getFullName() != null) {
                                names.add(s.getFullName());
                            }
                        }
                    }
                    items.add(new ShiftDisplayItem(entity, r.getAssignedCount(), r.getMinStaff(), names));
                }

                // Sắp xếp ca theo ngày tăng dần, sau đó theo giờ bắt đầu tăng dần
                java.util.Collections.sort(items, (a, b) -> {
                    int dateCmp = Long.compare(a.shift.getShiftDate(), b.shift.getShiftDate());
                    if (dateCmp != 0) return dateCmp;
                    return a.shift.getStartTime().compareTo(b.shift.getStartTime());
                });

                weekShiftsLive.setValue(items);
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                messageLive.setValue("Không thể tải lịch tuần: " + e.getMessage());
            }
        });
    }

    public void optimisticallyUpdateShiftStatus(int shiftId, String newStatus) {
        List<ShiftDisplayItem> current = weekShiftsLive.getValue();
        if (current == null) return;
        List<ShiftDisplayItem> updated = new ArrayList<>();
        for (ShiftDisplayItem item : current) {
            if (item.shift.getShiftId() == shiftId) {
                ShiftEntity updatedShift = new ShiftEntity();
                updatedShift.setShiftId(item.shift.getShiftId());
                updatedShift.setShiftName(item.shift.getShiftName());
                updatedShift.setShiftDate(item.shift.getShiftDate());
                updatedShift.setStartTime(item.shift.getStartTime());
                updatedShift.setEndTime(item.shift.getEndTime());
                updatedShift.setTemplateId(item.shift.getTemplateId());
                updatedShift.setStatus(newStatus);
                updated.add(new ShiftDisplayItem(
                    updatedShift,
                    item.assignedCount,
                    item.minStaff,
                    item.assignedStaffNames
                ));
            } else {
                updated.add(item);
            }
        }
        weekShiftsLive.setValue(updated);
    }

    public void optimisticallyAddStaff(int shiftId, String staffName) {
        List<ShiftDisplayItem> current = weekShiftsLive.getValue();
        if (current == null) return;
        List<ShiftDisplayItem> updated = new ArrayList<>();
        for (ShiftDisplayItem item : current) {
            if (item.shift.getShiftId() == shiftId) {
                List<String> newNames = new ArrayList<>(item.assignedStaffNames);
                newNames.add(staffName);
                updated.add(new ShiftDisplayItem(
                    item.shift,
                    item.assignedCount + 1,
                    item.minStaff,
                    newNames
                ));
            } else {
                updated.add(item);
            }
        }
        weekShiftsLive.setValue(updated);
    }

    public void optimisticallyRemoveStaff(int shiftId, String staffName) {
        List<ShiftDisplayItem> current = weekShiftsLive.getValue();
        if (current == null) return;
        List<ShiftDisplayItem> updated = new ArrayList<>();
        for (ShiftDisplayItem item : current) {
            if (item.shift.getShiftId() == shiftId) {
                List<String> newNames = new ArrayList<>(item.assignedStaffNames);
                newNames.remove(staffName);
                updated.add(new ShiftDisplayItem(
                    item.shift,
                    Math.max(0, item.assignedCount - 1),
                    item.minStaff,
                    newNames
                ));
            } else {
                updated.add(item);
            }
        }
        weekShiftsLive.setValue(updated);
    }
}