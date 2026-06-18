package com.example.cafe_manager.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "shift_templates", indices = {@Index(value = "template_name", unique = true)})
public class ShiftTemplateEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "template_id")
    private int templateId;

    @ColumnInfo(name = "template_name")
    private String templateName;

    @ColumnInfo(name = "start_time")
    private String startTime; // Định dạng HH:mm, ví dụ: "06:00"

    @ColumnInfo(name = "end_time")
    private String endTime; // Định dạng HH:mm, ví dụ: "14:00"

    @ColumnInfo(name = "min_staff")
    private int minStaff;

    @ColumnInfo(name = "is_active")
    private boolean isActive;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    public ShiftTemplateEntity() {}

    public int getTemplateId() { return templateId; }
    public void setTemplateId(int templateId) { this.templateId = templateId; }

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public int getMinStaff() { return minStaff; }
    public void setMinStaff(int minStaff) { this.minStaff = minStaff; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
