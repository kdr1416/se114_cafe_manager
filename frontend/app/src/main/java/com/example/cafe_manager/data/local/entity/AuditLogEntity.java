package com.example.cafe_manager.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "audit_logs")
public class AuditLogEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "log_id")
    private int logId;

    @ColumnInfo(name = "user_id")
    private int userId;

    @ColumnInfo(name = "action")
    private String action;

    @ColumnInfo(name = "target_type")
    private String targetType;

    @ColumnInfo(name = "target_id")
    private String targetId;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    public AuditLogEntity() {}

    public int getLogId() { return logId; }
    public void setLogId(int logId) { this.logId = logId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
