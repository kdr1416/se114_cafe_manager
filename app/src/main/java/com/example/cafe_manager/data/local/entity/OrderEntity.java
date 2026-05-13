package com.example.cafe_manager.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "orders",
foreignKeys = @ForeignKey(entity = TableEntity.class,
parentColumns = "table_id",
childColumns = "table_id",
onDelete = ForeignKey.CASCADE),
indices = {@Index("table_id")})
public class OrderEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "order_id")
    private int orderId;

    @ColumnInfo(name = "table_id")
    private int tableId;

    @ColumnInfo(name = "order_code")
    private String orderCode;

    @ColumnInfo(name = "status")
    private String status;

    @ColumnInfo(name = "total_amount")
    private double totalAmount;

    @ColumnInfo(name = "note")
    private String note;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "paid_at")
    private long paidAt;

    public OrderEntity() {
    }

    public OrderEntity(int tableId, String orderCode, String status, double totalAmount,
                       String note, long createdAt, long paidAt) {
        this.tableId = tableId;
        this.orderCode = orderCode;
        this.status = status;
        this.totalAmount = totalAmount;
        this.note = note;
        this.createdAt = createdAt;
        this.paidAt = paidAt;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getTableId() {
        return tableId;
    }

    public void setTableId(int tableId) {
        this.tableId = tableId;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(long paidAt) {
        this.paidAt = paidAt;
    }
}
