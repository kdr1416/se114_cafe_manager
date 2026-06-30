package com.example.cafe_manager.data.remote;

public class AttendanceResponse {
    private Integer attendanceId;
    private Integer shiftId;
    private Integer userId;
    private String username;
    private Long checkInAt;
    private Long checkOutAt;
    private String status;
    private Integer lateMinutes;
    private Integer earlyLeaveMinutes;
    private String notes;

    public AttendanceResponse() {}

    public Integer getAttendanceId() {
        return attendanceId;
    }

    public void setAttendanceId(Integer attendanceId) {
        this.attendanceId = attendanceId;
    }

    public Integer getShiftId() {
        return shiftId;
    }

    public void setShiftId(Integer shiftId) {
        this.shiftId = shiftId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getCheckInAt() {
        return checkInAt;
    }

    public void setCheckInAt(Long checkInAt) {
        this.checkInAt = checkInAt;
    }

    public Long getCheckOutAt() {
        return checkOutAt;
    }

    public void setCheckOutAt(Long checkOutAt) {
        this.checkOutAt = checkOutAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getLateMinutes() {
        return lateMinutes;
    }

    public void setLateMinutes(Integer lateMinutes) {
        this.lateMinutes = lateMinutes;
    }

    public Integer getEarlyLeaveMinutes() {
        return earlyLeaveMinutes;
    }

    public void setEarlyLeaveMinutes(Integer earlyLeaveMinutes) {
        this.earlyLeaveMinutes = earlyLeaveMinutes;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
