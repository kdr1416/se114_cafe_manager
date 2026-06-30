package com.example.cafe_manager.data.remote;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

public class RevenueReportResponse {
    @SerializedName("year")
    private int year;

    @SerializedName("month")
    private int month;

    @SerializedName("totalRevenue")
    private Double totalRevenue;

    @SerializedName("orderCount")
    private Integer orderCount;

    @SerializedName("avgOrderValue")
    private Double avgOrderValue;

    @SerializedName("previousMonthRevenue")
    private Double previousMonthRevenue;

    @SerializedName("growthPercent")
    private Double growthPercent;

    @SerializedName("revenueByDay")
    private List<DailyRevenue> revenueByDay;

    @SerializedName("revenueByMethod")
    private Map<String, Double> revenueByMethod;

    @SerializedName("orderCountByMethod")
    private Map<String, Integer> orderCountByMethod;

    @SerializedName("revenueByMonth")
    private List<MonthlyRevenue> revenueByMonth;

    @SerializedName("totalItemsSold")
    private Integer totalItemsSold;

    @SerializedName("itemsSold")
    private List<ProductSoldResponse> itemsSold;

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public Double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; }

    public Integer getOrderCount() { return orderCount; }
    public void setOrderCount(Integer orderCount) { this.orderCount = orderCount; }

    public Double getAvgOrderValue() { return avgOrderValue; }
    public void setAvgOrderValue(Double avgOrderValue) { this.avgOrderValue = avgOrderValue; }

    public Double getPreviousMonthRevenue() { return previousMonthRevenue; }
    public void setPreviousMonthRevenue(Double previousMonthRevenue) { this.previousMonthRevenue = previousMonthRevenue; }

    public Double getGrowthPercent() { return growthPercent; }
    public void setGrowthPercent(Double growthPercent) { this.growthPercent = growthPercent; }

    public List<DailyRevenue> getRevenueByDay() { return revenueByDay; }
    public void setRevenueByDay(List<DailyRevenue> revenueByDay) { this.revenueByDay = revenueByDay; }

    public Map<String, Double> getRevenueByMethod() { return revenueByMethod; }
    public void setRevenueByMethod(Map<String, Double> revenueByMethod) { this.revenueByMethod = revenueByMethod; }

    public Map<String, Integer> getOrderCountByMethod() { return orderCountByMethod; }
    public void setOrderCountByMethod(Map<String, Integer> orderCountByMethod) { this.orderCountByMethod = orderCountByMethod; }

    public List<MonthlyRevenue> getRevenueByMonth() { return revenueByMonth; }
    public void setRevenueByMonth(List<MonthlyRevenue> revenueByMonth) { this.revenueByMonth = revenueByMonth; }

    public Integer getTotalItemsSold() { return totalItemsSold; }
    public void setTotalItemsSold(Integer totalItemsSold) { this.totalItemsSold = totalItemsSold; }

    public List<ProductSoldResponse> getItemsSold() { return itemsSold; }
    public void setItemsSold(List<ProductSoldResponse> itemsSold) { this.itemsSold = itemsSold; }

    public static class DailyRevenue {
        @SerializedName("day")
        private int day;

        @SerializedName("date")
        private String date;

        @SerializedName("revenue")
        private Double revenue;

        @SerializedName("orderCount")
        private Integer orderCount;

        public int getDay() { return day; }
        public void setDay(int day) { this.day = day; }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public Double getRevenue() { return revenue; }
        public void setRevenue(Double revenue) { this.revenue = revenue; }

        public Integer getOrderCount() { return orderCount; }
        public void setOrderCount(Integer orderCount) { this.orderCount = orderCount; }
    }

    public static class MonthlyRevenue {
        @SerializedName("month")
        private int month;

        @SerializedName("revenue")
        private Double revenue;

        @SerializedName("orderCount")
        private Integer orderCount;

        public int getMonth() { return month; }
        public void setMonth(int month) { this.month = month; }

        public Double getRevenue() { return revenue; }
        public void setRevenue(Double revenue) { this.revenue = revenue; }

        public Integer getOrderCount() { return orderCount; }
        public void setOrderCount(Integer orderCount) { this.orderCount = orderCount; }
    }

    public static class ProductSoldResponse {
        @SerializedName("productId")
        private Integer productId;

        @SerializedName("productName")
        private String productName;

        @SerializedName("quantity")
        private Integer quantity;

        @SerializedName("revenue")
        private Double revenue;

        public Integer getProductId() { return productId; }
        public void setProductId(Integer productId) { this.productId = productId; }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public Double getRevenue() { return revenue; }
        public void setRevenue(Double revenue) { this.revenue = revenue; }
    }
}
