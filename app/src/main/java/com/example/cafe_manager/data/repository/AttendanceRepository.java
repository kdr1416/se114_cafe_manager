package com.example.cafe_manager.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.dao.AttendanceDao;
import com.example.cafe_manager.data.local.dao.ShiftDao;
import com.example.cafe_manager.data.local.entity.AttendanceEntity;
import com.example.cafe_manager.data.local.entity.ShiftEntity;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.RepositoryCallback;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttendanceRepository {

    private final AttendanceDao attendanceDao;
    private final ShiftDao shiftDao;
    private final AppExecutors executors;

    public AttendanceRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.attendanceDao = db.attendanceDao();
        this.shiftDao = db.shiftDao();
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

                AttendanceEntity existing = attendanceDao.getByShiftAndUser(shiftId, userId);
                long checkInTime = System.currentTimeMillis();

                // Xác định trạng thái muộn (LATE)
                long shiftStartTime = parseTime(shift.getShiftDate(), shift.getStartTime());
                String status = "CHECKED_IN";
                if (checkInTime > shiftStartTime + (15 * 60 * 1000)) { // Muộn hơn 15 phút
                    status = "LATE";
                }

                if (existing == null) {
                    AttendanceEntity attendance = new AttendanceEntity();
                    attendance.setShiftId(shiftId);
                    attendance.setUserId(userId);
                    attendance.setCheckInAt(checkInTime);
                    attendance.setStatus(status);
                    attendanceDao.insert(attendance);
                } else {
                    if (existing.getCheckInAt() > 0) {
                        executors.mainThread().execute(() -> callback.onError(new Exception("Bạn đã check-in ca này rồi.")));
                        return;
                    }
                    attendanceDao.checkIn(shiftId, userId, checkInTime, status);
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
                long shiftStartTime = parseTime(shift.getShiftDate(), shift.getStartTime());
                long shiftEndTime = parseTime(shift.getShiftDate(), shift.getEndTime());

                // Tính toán số phút muộn và về sớm
                int lateMin = (attendance.getCheckInAt() > shiftStartTime) ? (int) ((attendance.getCheckInAt() - shiftStartTime) / 60000) : 0;
                int earlyLeaveMin = (checkOutTime < shiftEndTime) ? (int) ((shiftEndTime - checkOutTime) / 60000) : 0;

                String status = "COMPLETED";
                if (lateMin > 15) {
                    status = "LATE";
                }
                if (earlyLeaveMin > 15) {
                    status = "EARLY_LEAVE";
                }

                attendanceDao.checkOut(shiftId, userId, checkOutTime, lateMin, earlyLeaveMin, status);
                executors.mainThread().execute(() -> callback.onSuccess(null));
            } catch (Exception e) {
                executors.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    // ── Helper ────────────────────────────────────────────────────

    private long parseTime(long baseDate, String timeStr) {
        try {
            SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String dateStr = dayFormat.format(new Date(baseDate));
            SimpleDateFormat fullFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Date date = fullFormat.parse(dateStr + " " + timeStr);
            return date != null ? date.getTime() : baseDate;
        } catch (Exception e) {
            return baseDate;
        }
    }
}
