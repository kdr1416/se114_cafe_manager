package com.example.cafe_manager.data.remote;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DailyShiftReportResponse {
    @SerializedName("date")
    private String date;
    
    @SerializedName("totalRevenue")
    private Double totalRevenue;
    
    @SerializedName("totalOrders")
    private Integer totalOrders;
    
    @SerializedName("paymentCount")
    private Integer paymentCount;
    
    @SerializedName("totalOpeningCash")
    private Double totalOpeningCash;
    
    @SerializedName("totalExpectedCash")
    private Double totalExpectedCash;
    
    @SerializedName("shifts")
    private List<ShiftSummary> shifts;

    public DailyShiftReportResponse() {}

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public Double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; }

    public Integer getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Integer totalOrders) { this.totalOrders = totalOrders; }

    public Integer getPaymentCount() { return paymentCount; }
    public void setPaymentCount(Integer paymentCount) { this.paymentCount = paymentCount; }

    public Double getTotalOpeningCash() { return totalOpeningCash; }
    public void setTotalOpeningCash(Double totalOpeningCash) { this.totalOpeningCash = totalOpeningCash; }

    public Double getTotalExpectedCash() { return totalExpectedCash; }
    public void setTotalExpectedCash(Double totalExpectedCash) { this.totalExpectedCash = totalExpectedCash; }

    public List<ShiftSummary> getShifts() { return shifts; }
    public void setShifts(List<ShiftSummary> shifts) { this.shifts = shifts; }

    public static class ShiftSummary {
        @SerializedName("shiftId")
        private Integer shiftId;
        
        @SerializedName("shiftName")
        private String shiftName;
        
        @SerializedName("startTime")
        private String startTime;
        
        @SerializedName("endTime")
        private String endTime;
        
        @SerializedName("status")
        private String status;
        
        @SerializedName("revenue")
        private Double revenue;
        
        @SerializedName("orderCount")
        private Integer orderCount;
        
        @SerializedName("openingCash")
        private Double openingCash;
        
        @SerializedName("expectedCash")
        private Double expectedCash;

        public ShiftSummary() {}

        public Integer getShiftId() { return shiftId; }
        public void setShiftId(Integer shiftId) { this.shiftId = shiftId; }

        public String getShiftName() { return shiftName; }
        public void setShiftName(String shiftName) { this.shiftName = shiftName; }

        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }

        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Double getRevenue() { return revenue; }
        public void setRevenue(Double revenue) { this.revenue = revenue; }

        public Integer getOrderCount() { return orderCount; }
        public void setOrderCount(Integer orderCount) { this.orderCount = orderCount; }

        public Double getOpeningCash() { return openingCash; }
        public void setOpeningCash(Double openingCash) { this.openingCash = openingCash; }

        public Double getExpectedCash() { return expectedCash; }
        public void setExpectedCash(Double expectedCash) { this.expectedCash = expectedCash; }
    }
}
