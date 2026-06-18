package com.example.cafe_manager.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "attendance",
    indices = {@Index(value = {"shift_id", "user_id"}, unique = true)}
)
public class AttendanceEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "attendance_id")
    private int attendanceId;

    @ColumnInfo(name = "shift_id")
    private int shiftId;

    @ColumnInfo(name = "user_id")
    private int userId;

    @ColumnInfo(name = "check_in_at")
    private long checkInAt; // Epoch timestamp, 0 nếu chưa check-in

    @ColumnInfo(name = "check_out_at")
    private long checkOutAt; // Epoch timestamp, 0 nếu chưa check-out

    @ColumnInfo(name = "status")
    private String status; // ABSENT, CHECKED_IN, COMPLETED, LATE, EARLY_LEAVE

    @ColumnInfo(name = "late_minutes")
    private int lateMinutes;

    @ColumnInfo(name = "early_leave_minutes")
    private int earlyLeaveMinutes;

    @ColumnInfo(name = "notes")
    private String notes; // Ghi chú của quản lý hoặc nhân viên

    public AttendanceEntity() {}

    public int getAttendanceId() { return attendanceId; }
    public void setAttendanceId(int attendanceId) { this.attendanceId = attendanceId; }

    public int getShiftId() { return shiftId; }
    public void setShiftId(int shiftId) { this.shiftId = shiftId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public long getCheckInAt() { return checkInAt; }
    public void setCheckInAt(long checkInAt) { this.checkInAt = checkInAt; }

    public long getCheckOutAt() { return checkOutAt; }
    public void setCheckOutAt(long checkOutAt) { this.checkOutAt = checkOutAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getLateMinutes() { return lateMinutes; }
    public void setLateMinutes(int lateMinutes) { this.lateMinutes = lateMinutes; }

    public int getEarlyLeaveMinutes() { return earlyLeaveMinutes; }
    public void setEarlyLeaveMinutes(int earlyLeaveMinutes) { this.earlyLeaveMinutes = earlyLeaveMinutes; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
