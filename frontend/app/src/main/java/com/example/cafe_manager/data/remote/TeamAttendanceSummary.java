package com.example.cafe_manager.data.remote;

public class TeamAttendanceSummary {
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

    public TeamAttendanceSummary() {}

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
