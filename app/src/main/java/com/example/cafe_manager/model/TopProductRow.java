package com.example.cafe_manager.model;

import androidx.room.ColumnInfo;

/**
 * Projection POJO cho query top sản phẩm bán chạy.
 * Dùng ở Dashboard.
 */
public class TopProductRow {

    @ColumnInfo(name = "product_name")
    public String productName;

    @ColumnInfo(name = "total_quantity")
    public int totalQuantity;

    @ColumnInfo(name = "total_revenue")
    public double totalRevenue;
}
