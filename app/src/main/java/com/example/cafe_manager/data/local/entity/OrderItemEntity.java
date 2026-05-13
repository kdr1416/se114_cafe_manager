package com.example.cafe_manager.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "order_items",
foreignKeys ={
        @ForeignKey(entity = OrderEntity.class,
        parentColumns = "order_id",
        childColumns = "order_id"),
        @ForeignKey(entity = ProductEntity.class,
        parentColumns = "product_id",
        childColumns = "product_id")},
indices = {@Index("order_id"), @Index("product_id")})
public class OrderItemEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "order_item_id")
    private int orderItemId;

    @ColumnInfo(name = "order_id")
    private int orderId;

    @ColumnInfo(name = "product_id")
    private int productId;

    @ColumnInfo(name = "product_name_snapshot")
    private String productNameSnapshot;

    @ColumnInfo(name = "quantity")
    private int quantity;

    @ColumnInfo(name = "unit_price")
    private double unitPrice;

    @ColumnInfo(name = "subtotal")
    private double subtotal;

    @ColumnInfo(name = "note")
    private String note;

    public OrderItemEntity() {
    }

    public OrderItemEntity(int orderId, int productId, String productNameSnapshot, int quantity,
                           double unitPrice, double subtotal, String note) {
        this.orderId = orderId;
        this.productId = productId;
        this.productNameSnapshot = productNameSnapshot;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
        this.note = note;
    }

    public int getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(int orderItemId) {
        this.orderItemId = orderItemId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getProductNameSnapshot() {
        return productNameSnapshot;
    }

    public void setProductNameSnapshot(String productNameSnapshot) {
        this.productNameSnapshot = productNameSnapshot;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
