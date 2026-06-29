package com.example.cafe_manager.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class CategoryEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "category_id")
    private int categoryId;

    @ColumnInfo(name = "category_name")
    private String categoryName;

    @ColumnInfo(name = "is_active")
    private boolean isActive;

    @ColumnInfo(name = "description")
    private String description;

    public CategoryEntity() {
    }

    public CategoryEntity(String categoryName, boolean isActive, String description) {
        this.categoryName = categoryName;
        this.isActive = isActive;
        this.description = description;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
