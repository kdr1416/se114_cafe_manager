package com.example.cafe_manager.model;

import androidx.room.ColumnInfo;

public class DailyRevenueRow {

    @ColumnInfo(name = "day_label")
    public String dayLabel;

    @ColumnInfo(name = "daily_revenue")
    public double dailyRevenue;
}
