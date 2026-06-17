package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.local.entity.ShiftAssignmentEntity;
import com.example.cafe_manager.data.repository.AttendanceRepository;
import com.example.cafe_manager.data.repository.ShiftRepository;
import com.example.cafe_manager.util.RepositoryCallback;

import java.util.List;

public class MyShiftViewModel extends AndroidViewModel {

    private final ShiftRepository shiftRepository;
    private final AttendanceRepository attendanceRepository;
    private final MutableLiveData<String> message = new MutableLiveData<>();

    public MyShiftViewModel(@NonNull Application application) {
        super(application);
        this.shiftRepository = new ShiftRepository(application);
        this.attendanceRepository = new AttendanceRepository(application);
    }

    public LiveData<String> getMessage() { return message; }
    public void clearMessage() { message.setValue(null); }

    public LiveData<List<ShiftAssignmentEntity>> getMyAssignments(int userId) {
        return shiftRepository.getAssignmentsByUser(userId);
    }

    public void confirmShift(int assignmentId) {
        shiftRepository.confirmAssignment(assignmentId, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                message.setValue("Đã xác nhận tham gia ca.");
            }

            @Override
            public void onError(Exception e) {
                message.setValue("Xác nhận thất bại: " + e.getMessage());
            }
        });
    }

    public void checkIn(int shiftId, int userId) {
        attendanceRepository.checkIn(shiftId, userId, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                message.setValue("Check-in ca làm việc thành công.");
            }

            @Override
            public void onError(Exception e) {
                message.setValue("Check-in lỗi: " + e.getMessage());
            }
        });
    }

    public void checkOut(int shiftId, int userId) {
        attendanceRepository.checkOut(shiftId, userId, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                message.setValue("Check-out ca làm việc thành công.");
            }

            @Override
            public void onError(Exception e) {
                message.setValue("Check-out lỗi: " + e.getMessage());
            }
        });
    }
}
