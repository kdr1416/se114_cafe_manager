package com.example.cafe_manager.data.remote;

import java.util.List;

public class ShiftReportResponse {
    private int shiftId;
    private String shiftName;
    private Long shiftDate;
    private String status;
    private Double openingCash;
    private Double closingCash;
    private int totalOrders;
    private double totalRevenue;
    private List<UserProfileResponse> assignedStaff;

    // Expanded report fields
    private Double cashRevenue;
    private Double transferRevenue;
    private Double momoRevenue;
    private Integer unpaidOrders;
    private Integer paymentCount;
    private Double expectedCash;
    private Double cashDifference;
    private Integer staffPresentCount;
    private List<PaymentMethodStatsResponse> paymentMethodStats;
    private List<ProductSoldSummary> topProducts;
    private List<StaffAttendanceSummary> attendanceList;
    private List<ShiftOrderResponse> orderHistory;

    public ShiftReportResponse() {}

    public List<ShiftOrderResponse> getOrderHistory() { return orderHistory; }
    public void setOrderHistory(List<ShiftOrderResponse> orderHistory) { this.orderHistory = orderHistory; }

    public int getShiftId() { return shiftId; }
    public void setShiftId(int shiftId) { this.shiftId = shiftId; }

    public String getShiftName() { return shiftName; }
    public void setShiftName(String shiftName) { this.shiftName = shiftName; }

    public Long getShiftDate() { return shiftDate; }
    public void setShiftDate(Long shiftDate) { this.shiftDate = shiftDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getOpeningCash() { return openingCash; }
    public void setOpeningCash(Double openingCash) { this.openingCash = openingCash; }

    public Double getClosingCash() { return closingCash; }
    public void setClosingCash(Double closingCash) { this.closingCash = closingCash; }

    public int getTotalOrders() { return totalOrders; }
    public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }

    public double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }

    public List<UserProfileResponse> getAssignedStaff() { return assignedStaff; }
    public void setAssignedStaff(List<UserProfileResponse> assignedStaff) { this.assignedStaff = assignedStaff; }

    public Double getCashRevenue() { return cashRevenue; }
    public void setCashRevenue(Double cashRevenue) { this.cashRevenue = cashRevenue; }

    public Double getTransferRevenue() { return transferRevenue; }
    public void setTransferRevenue(Double transferRevenue) { this.transferRevenue = transferRevenue; }

    public Double getMomoRevenue() { return momoRevenue; }
    public void setMomoRevenue(Double momoRevenue) { this.momoRevenue = momoRevenue; }

    public Integer getUnpaidOrders() { return unpaidOrders; }
    public void setUnpaidOrders(Integer unpaidOrders) { this.unpaidOrders = unpaidOrders; }

    public Integer getPaymentCount() { return paymentCount; }
    public void setPaymentCount(Integer paymentCount) { this.paymentCount = paymentCount; }

    public Double getExpectedCash() { return expectedCash; }
    public void setExpectedCash(Double expectedCash) { this.expectedCash = expectedCash; }

    public Double getCashDifference() { return cashDifference; }
    public void setCashDifference(Double cashDifference) { this.cashDifference = cashDifference; }

    public Integer getStaffPresentCount() { return staffPresentCount; }
    public void setStaffPresentCount(Integer staffPresentCount) { this.staffPresentCount = staffPresentCount; }

    public List<PaymentMethodStatsResponse> getPaymentMethodStats() { return paymentMethodStats; }
    public void setPaymentMethodStats(List<PaymentMethodStatsResponse> paymentMethodStats) { this.paymentMethodStats = paymentMethodStats; }

    public List<ProductSoldSummary> getTopProducts() { return topProducts; }
    public void setTopProducts(List<ProductSoldSummary> topProducts) { this.topProducts = topProducts; }

    public List<StaffAttendanceSummary> getAttendanceList() { return attendanceList; }
    public void setAttendanceList(List<StaffAttendanceSummary> attendanceList) { this.attendanceList = attendanceList; }

    public static class PaymentMethodStatsResponse {
        private String paymentMethod;
        private Integer orderCount;
        private Double totalRevenue;

        public PaymentMethodStatsResponse() {}

        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

        public Integer getOrderCount() { return orderCount; }
        public void setOrderCount(Integer orderCount) { this.orderCount = orderCount; }

        public Double getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; }
    }

    public static class ProductSoldSummary {
        private Integer productId;
        private String productName;
        private Integer quantity;
        private Double subtotal;

        public ProductSoldSummary() {}

        public Integer getProductId() { return productId; }
        public void setProductId(Integer productId) { this.productId = productId; }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public Double getSubtotal() { return subtotal; }
        public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }
    }

    public static class StaffAttendanceSummary {
        private Integer userId;
        private String username;
        private String fullName;
        private Long checkInAt;
        private Long checkOutAt;
        private String status;
        private String notes;

        public StaffAttendanceSummary() {}

        public Integer getUserId() { return userId; }
        public void setUserId(Integer userId) { this.userId = userId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public Long getCheckInAt() { return checkInAt; }
        public void setCheckInAt(Long checkInAt) { this.checkInAt = checkInAt; }

        public Long getCheckOutAt() { return checkOutAt; }
        public void setCheckOutAt(Long checkOutAt) { this.checkOutAt = checkOutAt; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    public static class ShiftOrderResponse {
        private Integer orderId;
        private String orderCode;
        private String tableName;
        private Double totalAmount;
        private String status;
        private String paymentMethod;
        private Long paidAt;
        private String cashierName;

        public ShiftOrderResponse() {}

        public Integer getOrderId() { return orderId; }
        public void setOrderId(Integer orderId) { this.orderId = orderId; }

        public String getOrderCode() { return orderCode; }
        public void setOrderCode(String orderCode) { this.orderCode = orderCode; }

        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }

        public Double getTotalAmount() { return totalAmount; }
        public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

        public Long getPaidAt() { return paidAt; }
        public void setPaidAt(Long paidAt) { this.paidAt = paidAt; }

        public String getCashierName() { return cashierName; }
        public void setCashierName(String cashierName) { this.cashierName = cashierName; }
    }
}
