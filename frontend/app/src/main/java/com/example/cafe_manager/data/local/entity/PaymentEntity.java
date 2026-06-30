package com.example.cafe_manager.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "payments",
        indices = {@Index("order_id")}
)
public class PaymentEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "payment_id")
    private int paymentId;

    @ColumnInfo(name = "order_id")
    private int orderId;

    @ColumnInfo(name = "payment_method")
    private String paymentMethod;

    @ColumnInfo(name = "subtotal")
    private double subtotal;

    @ColumnInfo(name = "discount_amount")
    private double discountAmount;

    @ColumnInfo(name = "final_amount")
    private double finalAmount;

    @ColumnInfo(name = "paid_at")
    private long paidAt;

    @ColumnInfo(name = "status")
    private String status;
        
    @ColumnInfo(name = "cashier_user_id", defaultValue = "0")
    private int cashierUserId;

    @ColumnInfo(name = "paid_shift_id", defaultValue = "0")
    private int paidShiftId;

    public PaymentEntity() {
    }

    public PaymentEntity(int orderId, String paymentMethod, double subtotal, double discountAmount,
                         double finalAmount, long paidAt, String status) {
        this.orderId = orderId;
        this.paymentMethod = paymentMethod;
        this.subtotal = subtotal;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
        this.paidAt = paidAt;
        this.status = status;
    }

    public int getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(int paymentId) {
        this.paymentId = paymentId;
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

    public long getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(long paidAt) {
        this.paidAt = paidAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    public int getCashierUserId() { return cashierUserId; }
    public void setCashierUserId(int cashierUserId) { this.cashierUserId = cashierUserId; }

    public int getPaidShiftId() { return paidShiftId; }
    public void setPaidShiftId(int paidShiftId) { this.paidShiftId = paidShiftId; }

    @androidx.room.Ignore
    private String cashierFullName;

    public String getCashierFullName() { return cashierFullName; }
    public void setCashierFullName(String cashierFullName) { this.cashierFullName = cashierFullName; }
}
