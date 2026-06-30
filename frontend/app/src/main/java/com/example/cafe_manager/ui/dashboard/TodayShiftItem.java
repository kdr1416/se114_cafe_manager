package com.example.cafe_manager.ui.dashboard;

import com.example.cafe_manager.data.local.entity.AttendanceEntity;
import com.example.cafe_manager.data.local.entity.ShiftEntity;

public class TodayShiftItem {
    public final ShiftEntity shift;
    public final int staffCount; // Cho manager/admin
    public final AttendanceEntity attendance; // Cho staff
    public final boolean confirmed; // Cho staff
    public final int assignmentId; // Cho staff

    public TodayShiftItem(ShiftEntity shift, int staffCount, AttendanceEntity attendance, boolean confirmed, int assignmentId) {
        this.shift = shift;
        this.staffCount = staffCount;
        this.attendance = attendance;
        this.confirmed = confirmed;
        this.assignmentId = assignmentId;
    }
}
