package com.example.cafe_manager.data.remote;

public class PaymentRequest {
    private int orderId;
    private String paymentMethod;
    private String promotionCode;
    private double amountReceived;
    private Double discountAmount;

    public PaymentRequest(int orderId, String paymentMethod, String promotionCode, double amountReceived, Double discountAmount) {
        this.orderId = orderId;
        this.paymentMethod = paymentMethod;
        this.promotionCode = promotionCode;
        this.amountReceived = amountReceived;
        this.discountAmount = discountAmount;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPromotionCode() {
        return promotionCode;
    }

    public void setPromotionCode(String promotionCode) {
        this.promotionCode = promotionCode;
    }

    public double getAmountReceived() {
        return amountReceived;
    }

    public void setAmountReceived(double amountReceived) {
        this.amountReceived = amountReceived;
    }

    public Double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Double discountAmount) {
        this.discountAmount = discountAmount;
    }
}
