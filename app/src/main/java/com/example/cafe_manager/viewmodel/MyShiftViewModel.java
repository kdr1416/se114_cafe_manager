package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.entity.ShiftAssignmentEntity;
import com.example.cafe_manager.data.local.entity.ShiftEntity;
import com.example.cafe_manager.data.repository.ShiftRepository;
import com.example.cafe_manager.manager.SessionManager;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.RepositoryCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyShiftViewModel extends AndroidViewModel {

    private final ShiftRepository shiftRepository;
    private final AppDatabase appDatabase;
    private final int currentUserId;

    private final MutableLiveData<List<MyShiftItem>> myShiftsLive = new MutableLiveData<>();
    private final MutableLiveData<String> messageLive = new MutableLiveData<>();

    // ── Inner class ──

    public static class MyShiftItem {
        public final ShiftEntity shift;
        public final boolean confirmed;
        public final int assignmentId;

        public MyShiftItem(ShiftEntity shift, boolean confirmed, int assignmentId) {
            this.shift = shift;
            this.confirmed = confirmed;
            this.assignmentId = assignmentId;
        }
    }

    // ── Constructor ──

    public MyShiftViewModel(@NonNull Application application) {
        super(application);
        shiftRepository = new ShiftRepository(application);
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
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<ShiftAssignmentEntity> assignments =
                    appDatabase.shiftAssignmentDao().getByUserSync(currentUserId);
            List<MyShiftItem> items = new ArrayList<>();

            for (ShiftAssignmentEntity a : assignments) {
                ShiftEntity shift = appDatabase.shiftDao().getById(a.getShiftId());
                if (shift != null && !shift.getStatus().equals("CANCELLED")) {
                    items.add(new MyShiftItem(shift, a.isConfirmed(), a.getAssignmentId()));
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

    public void confirmAssignment(int assignmentId) {
        shiftRepository.confirmAssignment(assignmentId, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                messageLive.setValue("Đã xác nhận ca làm việc.");
                loadMyShifts(); // Refresh
            }
            @Override
            public void onError(Exception e) {
                messageLive.setValue("Lỗi: " + e.getMessage());
            }
        });
    }
}