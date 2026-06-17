package com.example.cafe_manager.model;

import androidx.room.ColumnInfo;

/**
 * Projection POJO cho query GROUP BY payment_method.
 * Dùng ở Dashboard.
 */
public class PaymentMethodStatsRow {

    @ColumnInfo(name = "payment_method")
    public String paymentMethod;

    @ColumnInfo(name = "order_count")
    public int orderCount;

    @ColumnInfo(name = "total_revenue")
    public double totalRevenue;
}
