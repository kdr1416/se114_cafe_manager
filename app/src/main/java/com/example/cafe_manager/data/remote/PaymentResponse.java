package com.example.cafe_manager.data.remote;

public class PaymentResponse {
    private int paymentId;
    private Integer orderId;
    private double subtotal;
    private double discountAmount;
    private double finalAmount;
    private double changeAmount;
    private Integer cashierUserId;
    private String cashierFullName;
    private String paymentMethod;
    private Long paidAt;
    private Integer paidShiftId;

    public int getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(int paymentId) {
        this.paymentId = paymentId;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public double getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(double finalAmount) {
        this.finalAmount = finalAmount;
    }

    public double getChangeAmount() {
        return changeAmount;
    }

    public void setChangeAmount(double changeAmount) {
        this.changeAmount = changeAmount;
    }

    public Integer getCashierUserId() {
        return cashierUserId;
    }

    public void setCashierUserId(Integer cashierUserId) {
        this.cashierUserId = cashierUserId;
    }

    public String getCashierFullName() {
        return cashierFullName;
    }

    public void setCashierFullName(String cashierFullName) {
        this.cashierFullName = cashierFullName;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Long getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Long paidAt) {
        this.paidAt = paidAt;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public Integer getPaidShiftId() {
        return paidShiftId;
    }

    public void setPaidShiftId(Integer paidShiftId) {
        this.paidShiftId = paidShiftId;
    }
}
