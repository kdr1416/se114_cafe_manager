package com.example.cafe_manager.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "areas")
public class AreaEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "area_id")
    private int areaId;

    @ColumnInfo(name = "area_name")
    private String areaName;

    @ColumnInfo(name = "prefix")
    private String prefix;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    public AreaEntity() {
    }

    @Ignore
    public AreaEntity(String areaName, String prefix, long createdAt) {
        this.areaName = areaName;
        this.prefix = prefix;
        this.createdAt = createdAt;
    }

    public int getAreaId() {
        return areaId;
    }

    public void setAreaId(int areaId) {
        this.areaId = areaId;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    // Transient fields for API responses (not persisted in Room)
    private String description;
    private int tableCount;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getTableCount() {
        return tableCount;
    }

    public void setTableCount(int tableCount) {
        this.tableCount = tableCount;
    }
}
