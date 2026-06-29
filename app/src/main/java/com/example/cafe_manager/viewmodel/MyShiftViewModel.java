package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.entity.AttendanceEntity;
import com.example.cafe_manager.data.local.entity.ShiftAssignmentEntity;
import com.example.cafe_manager.data.local.entity.ShiftEntity;
import com.example.cafe_manager.data.repository.AttendanceRepository;
import com.example.cafe_manager.data.repository.ShiftRepository;
import com.example.cafe_manager.data.repository.ChatRepository;
import com.example.cafe_manager.manager.SessionManager;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.RepositoryCallback;
import com.example.cafe_manager.util.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyShiftViewModel extends AndroidViewModel {

    private final ShiftRepository shiftRepository;
    private final AttendanceRepository attendanceRepository;
    private final AppDatabase appDatabase;
    private final int currentUserId;

    private final MutableLiveData<List<MyShiftItem>> myShiftsLive = new MutableLiveData<>();
    private final MutableLiveData<String> messageLive = new MutableLiveData<>();

    // ── Inner class ──

    public static class MyShiftItem {
        public final ShiftEntity shift;
        public final boolean confirmed;
        public final int assignmentId;
        public final AttendanceEntity attendance;

        public MyShiftItem(ShiftEntity shift, boolean confirmed, int assignmentId, AttendanceEntity attendance) {
            this.shift = shift;
            this.confirmed = confirmed;
            this.assignmentId = assignmentId;
            this.attendance = attendance;
        }
    }

    // ── Constructor ──

    public MyShiftViewModel(@NonNull Application application) {
        super(application);
        shiftRepository = ShiftRepository.getInstance(application);
        attendanceRepository = AttendanceRepository.getInstance(application);
        appDatabase = AppDatabase.getInstance(application);
        currentUserId = SessionManager.getInstance(application).getUserId();
        loadMyShifts();
    }

    // ── Getters ──

    public LiveData<List<MyShiftItem>> getMyShifts() { return myShiftsLive; }
    public LiveData<String> getMessage() { return messageLive; }
    public void clearMessage() { messageLive.setValue(null); }

    // ── Load ──

    public void loadMyShifts() {
        shiftRepository.syncMyShiftsAndAssignments(new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                attendanceRepository.syncMyAttendances(new RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void res) {
                        loadLocalShifts();
                    }

                    @Override
                    public void onError(Exception e) {
                        android.util.Log.e("MyShiftViewModel", "Lỗi đồng bộ điểm danh: " + e.getMessage());
                        loadLocalShifts();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                android.util.Log.e("MyShiftViewModel", "Lỗi đồng bộ ca làm việc: " + e.getMessage());
                attendanceRepository.syncMyAttendances(new RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void res) {
                        loadLocalShifts();
                    }

                    @Override
                    public void onError(Exception e1) {
                        loadLocalShifts();
                    }
                });
            }
        });
    }

    private void loadLocalShifts() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<ShiftAssignmentEntity> assignments =
                    appDatabase.shiftAssignmentDao().getByUserSync(currentUserId);
            List<MyShiftItem> items = new ArrayList<>();

            for (ShiftAssignmentEntity a : assignments) {
                ShiftEntity shift = appDatabase.shiftDao().getById(a.getShiftId());
                if (shift != null && (shift.getStatus().equals(Constants.SHIFT_PUBLISHED)
                        || shift.getStatus().equals(Constants.SHIFT_IN_PROGRESS)
                        || shift.getStatus().equals(Constants.SHIFT_CLOSED))) {
                    AttendanceEntity attendance = appDatabase.attendanceDao().getByShiftAndUser(a.getShiftId(), currentUserId);
                    items.add(new MyShiftItem(shift, a.isConfirmed(), a.getAssignmentId(), attendance));
                }
            }

            // Sắp xếp: ngày gần nhất ở trên
            Collections.sort(items, (a, b) -> {
                int cmp = Long.compare(b.shift.getShiftDate(), a.shift.getShiftDate());
                if (cmp != 0) return cmp;
                return a.shift.getStartTime().compareTo(b.shift.getStartTime());
            });

            AppExecutors.getInstance().mainThread().execute(() -> myShiftsLive.setValue(items));
        });
    }

    // ── Actions ──

    public void checkIn(int shiftId) {
        attendanceRepository.checkIn(shiftId, currentUserId, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                messageLive.setValue("Check-in thành công.");
                loadMyShifts();
            }

            @Override
            public void onError(Exception e) {
                messageLive.setValue("Lỗi check-in: " + e.getMessage());
            }
        });
    }

    public void checkOut(int shiftId) {
        attendanceRepository.checkOut(shiftId, currentUserId, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                messageLive.setValue("Check-out thành công.");
                loadMyShifts();
            }

            @Override
            public void onError(Exception e) {
                messageLive.setValue("Lỗi check-out: " + e.getMessage());
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

    public String getShiftName(int shiftId) {
        List<MyShiftItem> items = myShiftsLive.getValue();
        if (items != null) {
            for (MyShiftItem item : items) {
                if (item.shift.getShiftId() == shiftId) {
                    return item.shift.getShiftName();
                }
            }
        }
        return "Trò chuyện Ca";
    }
}