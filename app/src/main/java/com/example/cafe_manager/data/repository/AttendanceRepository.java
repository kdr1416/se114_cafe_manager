package com.example.cafe_manager.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.dao.AttendanceDao;
import com.example.cafe_manager.data.local.dao.ShiftAssignmentDao;
import com.example.cafe_manager.data.local.dao.ShiftDao;
import com.example.cafe_manager.data.local.entity.AttendanceEntity;
import com.example.cafe_manager.data.local.entity.ShiftAssignmentEntity;
import com.example.cafe_manager.data.local.entity.ShiftEntity;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.RepositoryCallback;
import com.example.cafe_manager.util.ShiftTimeUtils;

import java.util.List;

public class AttendanceRepository {

    private final AttendanceDao attendanceDao;
    private final ShiftDao shiftDao;
    private final ShiftAssignmentDao assignmentDao;
    private final AppExecutors executors;

    public AttendanceRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.attendanceDao = db.attendanceDao();
        this.shiftDao = db.shiftDao();
        this.assignmentDao = db.shiftAssignmentDao();
        this.executors = AppExecutors.getInstance();
    }

    public LiveData<List<AttendanceEntity>> getAttendanceForShift(int shiftId) {
        return attendanceDao.getByShift(shiftId);
    }

    public LiveData<List<AttendanceEntity>> getMyAttendance(int userId) {
        return attendanceDao.getByUser(userId);
    }

    public void checkIn(int shiftId, int userId, RepositoryCallback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                ShiftEntity shift = shiftDao.getById(shiftId);
                if (shift == null) {
                    executors.mainThread().execute(() -> callback.onError(new Exception("Ca làm việc không tồn tại.")));
                    return;
                }

                // Chặn check-in ca bị CANCELLED
                if (Constants.SHIFT_CANCELLED.equals(shift.getStatus())) {
                    executors.mainThread().execute(() -> callback.onError(new Exception("Ca làm việc này đã bị hủy.")));
                    return;
                }

                // Chỉ cho phép check-in với ca PUBLISHED hoặc IN_PROGRESS
                if (!Constants.SHIFT_PUBLISHED.equals(shift.getStatus()) && !Constants.SHIFT_IN_PROGRESS.equals(shift.getStatus())) {
                    executors.mainThread().execute(() -> callback.onError(new Exception("Chỉ có thể check-in cho ca đã phát hành hoặc đang chạy.")));
                    return;
                }

                // Kiểm tra phân công nhân viên
                ShiftAssignmentEntity assignment = assignmentDao.getByShiftAndUser(shiftId, userId);
                if (assignment == null) {
                    executors.mainThread().execute(() -> callback.onError(new Exception("Bạn không được phân công ca làm việc này.")));
                    return;
                }

                // Kiểm tra xem nhân sự đã xác nhận phân công chưa
                if (!assignment.isConfirmed()) {
                    executors.mainThread().execute(() -> callback.onError(new Exception("Bạn cần xác nhận ca làm việc trước khi check-in.")));
                    return;
                }

                AttendanceEntity existing = attendanceDao.getByShiftAndUser(shiftId, userId);
                long checkInTime = System.currentTimeMillis();

                // Xác định trạng thái muộn (LATE)
                long shiftStartTime = ShiftTimeUtils.getShiftStartMillis(shift.getShiftDate(), shift.getStartTime());
                String status = Constants.ATTENDANCE_CHECKED_IN;
                if (checkInTime > shiftStartTime + (15 * 60 * 1000)) { // Muộn hơn 15 phút
                    status = Constants.ATTENDANCE_LATE;
                }

                int lateMin = (checkInTime > shiftStartTime) ? (int) ((checkInTime - shiftStartTime) / 60000) : 0;

                if (existing == null) {
                    AttendanceEntity attendance = new AttendanceEntity();
                    attendance.setShiftId(shiftId);
                    attendance.setUserId(userId);
                    attendance.setCheckInAt(checkInTime);
                    attendance.setLateMinutes(lateMin);
                    attendance.setStatus(status);
                    attendanceDao.insert(attendance);
                } else {
                    if (existing.getCheckInAt() > 0) {
                        executors.mainThread().execute(() -> callback.onError(new Exception("Bạn đã check-in ca này rồi.")));
                        return;
                    }
                    existing.setCheckInAt(checkInTime);
                    existing.setLateMinutes(lateMin);
                    existing.setStatus(status);
                    attendanceDao.update(existing);
                }
                executors.mainThread().execute(() -> callback.onSuccess(null));
            } catch (Exception e) {
                executors.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    public void checkOut(int shiftId, int userId, RepositoryCallback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                ShiftEntity shift = shiftDao.getById(shiftId);
                AttendanceEntity attendance = attendanceDao.getByShiftAndUser(shiftId, userId);
                if (shift == null || attendance == null) {
                    executors.mainThread().execute(() -> callback.onError(new Exception("Dữ liệu điểm danh không hợp lệ.")));
                    return;
                }

                if (attendance.getCheckInAt() == 0) {
                    executors.mainThread().execute(() -> callback.onError(new Exception("Bạn phải check-in trước khi check-out.")));
                    return;
                }
                if (attendance.getCheckOutAt() > 0) {
                    executors.mainThread().execute(() -> callback.onError(new Exception("Bạn đã check-out ca này rồi.")));
                    return;
                }

                long checkOutTime = System.currentTimeMillis();
                long shiftStartTime = ShiftTimeUtils.getShiftStartMillis(shift.getShiftDate(), shift.getStartTime());
                long shiftEndTime = ShiftTimeUtils.getShiftEndMillis(shift.getShiftDate(), shift.getStartTime(), shift.getEndTime());

                // Tính toán số phút muộn và về sớm
                int lateMin = (attendance.getCheckInAt() > shiftStartTime) ? (int) ((attendance.getCheckInAt() - shiftStartTime) / 60000) : 0;
                int earlyLeaveMin = (checkOutTime < shiftEndTime) ? (int) ((shiftEndTime - checkOutTime) / 60000) : 0;

                String status = Constants.ATTENDANCE_COMPLETED;
                if (lateMin > 15) {
                    status = Constants.ATTENDANCE_LATE;
                }
                if (earlyLeaveMin > 15) {
                    status = Constants.ATTENDANCE_EARLY_LEAVE;
                }

                attendance.setCheckOutAt(checkOutTime);
                attendance.setLateMinutes(lateMin);
                attendance.setEarlyLeaveMinutes(earlyLeaveMin);
                attendance.setStatus(status);
                attendanceDao.update(attendance);

                executors.mainThread().execute(() -> callback.onSuccess(null));
            } catch (Exception e) {
                executors.mainThread().execute(() -> callback.onError(e));
            }
        });
    }
}
