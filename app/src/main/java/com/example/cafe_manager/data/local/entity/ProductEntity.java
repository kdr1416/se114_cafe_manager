package com.example.cafe_manager.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "products",
foreignKeys = @ForeignKey(entity = CategoryEntity.class,
parentColumns = "category_id",
childColumns = "category_id",
onDelete = ForeignKey.CASCADE),
indices = {@Index("category_id")})
public class ProductEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "product_id")
    private int productId;

    @ColumnInfo(name = "category_id")
    private int categoryId;

    @ColumnInfo(name = "product_name")
    private String productName;

    @ColumnInfo(name = "price")
    private double price;

    @ColumnInfo(name = "image_url")
    private String imageUrl;

    @ColumnInfo(name = "is_active")
    private boolean isActive;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    public ProductEntity() {
    }

    public ProductEntity(int categoryId, String productName, double price, String imageUrl,
                         boolean isActive, long createdAt) {
        this.categoryId = categoryId;
        this.productName = productName;
        this.price = price;
        this.imageUrl = imageUrl;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
