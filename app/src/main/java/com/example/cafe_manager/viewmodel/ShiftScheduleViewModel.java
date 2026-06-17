package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.local.entity.ShiftAssignmentEntity;
import com.example.cafe_manager.data.local.entity.ShiftEntity;
import com.example.cafe_manager.data.repository.ShiftRepository;
import com.example.cafe_manager.util.RepositoryCallback;

import java.util.Calendar;
import java.util.List;

public class ShiftScheduleViewModel extends AndroidViewModel {

    private final ShiftRepository repository;
    private final MutableLiveData<Long> selectedDate = new MutableLiveData<>();
    private final MutableLiveData<String> message = new MutableLiveData<>();
    private LiveData<List<ShiftEntity>> shifts;

    public ShiftScheduleViewModel(@NonNull Application application) {
        super(application);
        this.repository = new ShiftRepository(application);
        
        // Mặc định chọn hôm nay (midnight)
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        selectedDate.setValue(cal.getTimeInMillis());
    }

    public LiveData<Long> getSelectedDate() { return selectedDate; }
    public LiveData<String> getMessage() { return message; }
    public void clearMessage() { message.setValue(null); }

    public void selectDate(long dateMidnight) {
        selectedDate.setValue(dateMidnight);
    }

    public LiveData<List<ShiftEntity>> getShiftsForSelectedDate() {
        shifts = repository.getShiftsByDate(selectedDate.getValue());
        return shifts;
    }

    public void createShift(String name, String start, String end, Integer templateId) {
        ShiftEntity shift = new ShiftEntity();
        shift.setShiftName(name);
        shift.setStartTime(start);
        shift.setEndTime(end);
        shift.setTemplateId(templateId);
        shift.setShiftDate(selectedDate.getValue());
        shift.setStatus("DRAFT");
        shift.setCreatedAt(System.currentTimeMillis());

        repository.insertShift(shift, new RepositoryCallback<Long>() {
            @Override
            public void onSuccess(Long result) {
                message.setValue("Đã tạo ca làm việc mới.");
            }

            @Override
            public void onError(Exception e) {
                message.setValue("Lỗi tạo ca: " + e.getMessage());
            }
        });
    }

    public void publishShift(int shiftId) {
        repository.openShift(shiftId, 0, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                message.setValue("Ca đã được công bố cho nhân viên.");
            }

            @Override
            public void onError(Exception e) {
                message.setValue("Lỗi: " + e.getMessage());
            }
        });
    }

    public void openShift(int shiftId, int managerId) {
        repository.openShift(shiftId, managerId, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                message.setValue("Đã mở ca làm việc & phiên bán hàng.");
            }

            @Override
            public void onError(Exception e) {
                message.setValue("Không thể mở ca: " + e.getMessage());
            }
        });
    }

    public void assignStaff(int shiftId, int userId, String role, int managerId) {
        ShiftAssignmentEntity assignment = new ShiftAssignmentEntity();
        assignment.setShiftId(shiftId);
        assignment.setUserId(userId);
        assignment.setRole(role);
        assignment.setAssignedBy(managerId);
        assignment.setConfirmed(false);
        assignment.setCreatedAt(System.currentTimeMillis());

        repository.assignStaff(assignment, new RepositoryCallback<Long>() {
            @Override
            public void onSuccess(Long result) {
                message.setValue("Đã phân công nhân viên.");
            }

            @Override
            public void onError(Exception e) {
                message.setValue("Lỗi phân công: " + e.getMessage());
            }
        });
    }

    public void removeStaff(int assignmentId) {
        repository.removeAssignment(assignmentId, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                message.setValue("Đã hủy phân công nhân viên.");
            }

            @Override
            public void onError(Exception e) {
                message.setValue("Lỗi: " + e.getMessage());
            }
        });
    }
}
