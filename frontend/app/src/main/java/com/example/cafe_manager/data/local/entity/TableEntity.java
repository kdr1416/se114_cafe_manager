package com.example.cafe_manager.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tables")
public class TableEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "table_id")
    private int tableId;

    @ColumnInfo(name = "table_name")
    private String tableName;

    @ColumnInfo(name = "status")
    private String status;

    @ColumnInfo(name = "capacity")
    private int capacity;

    @ColumnInfo(name = "area")
    private String area;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    public TableEntity() {
    }

    public TableEntity(String tableName, String status, int capacity, String area, long createdAt) {
        this.tableName = tableName;
        this.status = status;
        this.capacity = capacity;
        this.area = area;
        this.createdAt = createdAt;
    }

    public int getTableId() {
        return tableId;
    }

    public void setTableId(int tableId) {
        this.tableId = tableId;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
