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
    private final MediatorLiveData<List<ShiftDisplayItem>> shiftsLive = new MediatorLiveData<>();
    private final LiveData<List<ShiftTemplateEntity>> templatesLive;
    private final MutableLiveData<String> statusFilter = new MutableLiveData<>("ALL");
    private final MutableLiveData<String> messageLive = new MutableLiveData<>();

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

        // Enrich và lọc mỗi shift
        shiftsLive.addSource(rawShifts, shifts -> fetchAssignmentCountsForShifts(shifts));
        shiftsLive.addSource(statusFilter, filter -> reloadAndFilterShifts());
        shiftsLive.addSource(templatesLive, templates -> reloadAndFilterShifts());

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

    private final java.util.Map<Integer, List<String>> shiftAssignmentStaffNames = new java.util.HashMap<>();

    private void fetchAssignmentCountsForShifts(List<ShiftEntity> shifts) {
        if (shifts == null || shifts.isEmpty()) {
            shiftAssignmentStaffNames.clear();
            reloadAndFilterShifts();
            return;
        }

        final int total = shifts.size();
        final java.util.concurrent.atomic.AtomicInteger completed = new java.util.concurrent.atomic.AtomicInteger(0);

        for (ShiftEntity s : shifts) {
            shiftRepository.getAssignmentsForShift(s.getShiftId(), new RepositoryCallback<List<ShiftAssignmentResponse>>() {
                @Override
                public void onSuccess(List<ShiftAssignmentResponse> result) {
                    List<String> names = new ArrayList<>();
                    if (result != null) {
                        for (ShiftAssignmentResponse r : result) {
                            if (r.getFullName() != null) {
                                names.add(r.getFullName());
                            }
                        }
                    }
                    shiftAssignmentStaffNames.put(s.getShiftId(), names);
                    if (completed.incrementAndGet() == total) {
                        reloadAndFilterShifts();
                    }
                }

                @Override
                public void onError(Exception e) {
                    shiftAssignmentStaffNames.put(s.getShiftId(), new ArrayList<>());
                    if (completed.incrementAndGet() == total) {
                        reloadAndFilterShifts();
                    }
                }
            });
        }
    }

    private void reloadAndFilterShifts() {
        List<ShiftEntity> shifts = rawShifts.getValue();
        String filter = statusFilter.getValue();
        if (shifts == null) {
            shiftsLive.setValue(null);
            return;
        }

        List<ShiftTemplateEntity> templates = templatesLive.getValue();
        List<ShiftDisplayItem> items = new ArrayList<>();
        for (ShiftEntity s : shifts) {
            // Lọc theo status
            if (filter != null && !"ALL".equals(filter)) {
                if (!filter.equals(s.getStatus())) {
                    continue;
                }
            }

            List<String> names = shiftAssignmentStaffNames.get(s.getShiftId());
            if (names == null) names = new ArrayList<>();
            int count = names.size();
            int minStaff = 0;
            if (s.getTemplateId() != null && templates != null) {
                for (ShiftTemplateEntity t : templates) {
                    if (t.getTemplateId() == s.getTemplateId()) {
                        minStaff = t.getMinStaff();
                        break;
                    }
                }
            }
            items.add(new ShiftDisplayItem(s, count, minStaff, names));
        }
        shiftsLive.setValue(items);
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

        createShiftsRecursively(templates, date, 0, 0);
    }

    private void createShiftsRecursively(List<ShiftTemplateEntity> templates, long date, int index, int successCount) {
        if (index >= templates.size()) {
            shiftRepository.refreshShiftsFromApi(new RepositoryCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    messageLive.setValue("Đã tạo " + successCount + " ca làm việc.");
                }

                @Override
                public void onError(Exception e) {
                    messageLive.setValue("Đã tạo " + successCount + " ca làm việc (Lỗi đồng bộ: " + e.getMessage() + ")");
                }
            });
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

        shiftRepository.insertShiftNoRefresh(shift, new RepositoryCallback<Long>() {
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
            selectedDate.setValue(targetDate);
            shiftRepository.refreshShiftsFromApi(new RepositoryCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    messageLive.setValue("Đã sao chép " + successCount + " ca làm việc (chỉ khung ca).");
                }

                @Override
                public void onError(Exception e) {
                    messageLive.setValue("Đã sao chép " + successCount + " ca làm việc (Lỗi đồng bộ: " + e.getMessage() + ")");
                }
            });
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

        shiftRepository.insertShiftNoRefresh(newShift, new RepositoryCallback<Long>() {
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

    public LiveData<List<ShiftDisplayItem>> loadShiftsForDate(long date) {
        MediatorLiveData<List<ShiftDisplayItem>> result = new MediatorLiveData<>();
        LiveData<List<ShiftEntity>> rawDayShifts = shiftRepository.getShiftsByDate(date);

        Runnable reloadFilter = () -> {
            List<ShiftEntity> shifts = rawDayShifts.getValue();
            if (shifts == null) {
                result.setValue(new ArrayList<>());
                return;
            }
            final int total = shifts.size();
            if (total == 0) {
                result.setValue(new ArrayList<>());
                return;
            }

            final java.util.Map<Integer, List<String>> staffNamesMap = new java.util.HashMap<>();
            List<ShiftTemplateEntity> templates = templatesLive.getValue();

            // 1. Post immediately so shifts show up instantly on the UI
            postEnrichedItems(result, shifts, staffNamesMap, templates);

            // 2. Fetch assignment counts asynchronously and update dynamically
            for (ShiftEntity s : shifts) {
                shiftRepository.getAssignmentsForShift(s.getShiftId(), new RepositoryCallback<List<ShiftAssignmentResponse>>() {
                    @Override
                    public void onSuccess(List<ShiftAssignmentResponse> assignmentList) {
                        List<String> names = new ArrayList<>();
                        if (assignmentList != null) {
                            for (ShiftAssignmentResponse r : assignmentList) {
                                if (r.getFullName() != null) {
                                    names.add(r.getFullName());
                                }
                            }
                        }
                        staffNamesMap.put(s.getShiftId(), names);
                        postEnrichedItems(result, shifts, staffNamesMap, templates);
                    }

                    @Override
                    public void onError(Exception e) {
                        // Keep default of empty
                    }
                });
            }
        };

        result.addSource(rawDayShifts, shifts -> reloadFilter.run());
        result.addSource(statusFilter, filter -> reloadFilter.run());
        result.addSource(templatesLive, templates -> reloadFilter.run());

        return result;
    }

    private void postEnrichedItems(MediatorLiveData<List<ShiftDisplayItem>> liveData,
                                   List<ShiftEntity> shifts,
                                   java.util.Map<Integer, List<String>> staffNamesMap,
                                   List<ShiftTemplateEntity> templates) {
        String filter = statusFilter.getValue();
        List<ShiftDisplayItem> items = new ArrayList<>();
        for (ShiftEntity s : shifts) {
            if (filter != null && !"ALL".equals(filter) && !filter.equals(s.getStatus())) {
                continue;
            }
            List<String> names = staffNamesMap.get(s.getShiftId());
            if (names == null) names = new ArrayList<>();
            int count = names.size();
            int minStaff = 0;
            if (s.getTemplateId() != null && templates != null) {
                for (ShiftTemplateEntity t : templates) {
                    if (t.getTemplateId() == s.getTemplateId()) {
                        minStaff = t.getMinStaff();
                        break;
                    }
                }
            }
            items.add(new ShiftDisplayItem(s, count, minStaff, names));
        }
        liveData.setValue(items);
    }
}