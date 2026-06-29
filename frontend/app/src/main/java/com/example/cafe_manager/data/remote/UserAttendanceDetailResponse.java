package com.example.cafe_manager.data.remote;

import java.util.List;

public class UserAttendanceDetailResponse {
    private Integer userId;
    private String fullName;
    private String username;
    private String role;
    private Integer totalShifts;
    private Integer attendedShifts;
    private Integer absentShifts;
    private Integer lateCount;
    private Integer earlyLeaveCount;
    private Double totalHoursWorked;
    private Double attendanceRate;
    private Integer ordersCreated;
    private Integer paymentsProcessed;
    private Double revenueProcessed;
    private List<AttendanceRecord> records;

    public UserAttendanceDetailResponse() {}

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getTotalShifts() {
        return totalShifts;
    }

    public void setTotalShifts(Integer totalShifts) {
        this.totalShifts = totalShifts;
    }

    public Integer getAttendedShifts() {
        return attendedShifts;
    }

    public void setAttendedShifts(Integer attendedShifts) {
        this.attendedShifts = attendedShifts;
    }

    public Integer getAbsentShifts() {
        return absentShifts;
    }

    public void setAbsentShifts(Integer absentShifts) {
        this.absentShifts = absentShifts;
    }

    public Integer getLateCount() {
        return lateCount;
    }

    public void setLateCount(Integer lateCount) {
        this.lateCount = lateCount;
    }

    public Integer getEarlyLeaveCount() {
        return earlyLeaveCount;
    }

    public void setEarlyLeaveCount(Integer earlyLeaveCount) {
        this.earlyLeaveCount = earlyLeaveCount;
    }

    public Double getTotalHoursWorked() {
        return totalHoursWorked;
    }

    public void setTotalHoursWorked(Double totalHoursWorked) {
        this.totalHoursWorked = totalHoursWorked;
    }

    public Double getAttendanceRate() {
        return attendanceRate;
    }

    public void setAttendanceRate(Double attendanceRate) {
        this.attendanceRate = attendanceRate;
    }

    public List<AttendanceRecord> getRecords() {
        return records;
    }

    public void setRecords(List<AttendanceRecord> records) {
        this.records = records;
    }

    public Integer getOrdersCreated() {
        return ordersCreated;
    }

    public void setOrdersCreated(Integer ordersCreated) {
        this.ordersCreated = ordersCreated;
    }

    public Integer getPaymentsProcessed() {
        return paymentsProcessed;
    }

    public void setPaymentsProcessed(Integer paymentsProcessed) {
        this.paymentsProcessed = paymentsProcessed;
    }

    public Double getRevenueProcessed() {
        return revenueProcessed;
    }

    public void setRevenueProcessed(Double revenueProcessed) {
        this.revenueProcessed = revenueProcessed;
    }

    public static class AttendanceRecord {
        private Integer shiftId;
        private String shiftName;
        private Long shiftDate;
        private String startTime;
        private String endTime;
        private Double durationHours;
        private Long checkInAt;
        private Long checkOutAt;
        private Integer lateMinutes;
        private Integer earlyLeaveMinutes;
        private String status;
        private String notes;
        private Integer ordersCreated;
        private Integer paymentsProcessed;
        private Double revenueProcessed;

        public AttendanceRecord() {}

        public Integer getShiftId() {
            return shiftId;
        }

        public void setShiftId(Integer shiftId) {
            this.shiftId = shiftId;
        }

        public String getShiftName() {
            return shiftName;
        }

        public void setShiftName(String shiftName) {
            this.shiftName = shiftName;
        }

        public Long getShiftDate() {
            return shiftDate;
        }

        public void setShiftDate(Long shiftDate) {
            this.shiftDate = shiftDate;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }

        public Double getDurationHours() {
            return durationHours;
        }

        public void setDurationHours(Double durationHours) {
            this.durationHours = durationHours;
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

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public Integer getOrdersCreated() {
            return ordersCreated;
        }

        public void setOrdersCreated(Integer ordersCreated) {
            this.ordersCreated = ordersCreated;
        }

        public Integer getPaymentsProcessed() {
            return paymentsProcessed;
        }

        public void setPaymentsProcessed(Integer paymentsProcessed) {
            this.paymentsProcessed = paymentsProcessed;
        }

        public Double getRevenueProcessed() {
            return revenueProcessed;
        }

        public void setRevenueProcessed(Double revenueProcessed) {
            this.revenueProcessed = revenueProcessed;
        }
    }
}
