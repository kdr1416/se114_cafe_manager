package com.example.cafe_manager.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "shifts")
public class ShiftEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "shift_id")
    private int shiftId;

    @ColumnInfo(name = "template_id")
    private Integer templateId;

    @ColumnInfo(name = "shift_name")
    private String shiftName;

    @ColumnInfo(name = "shift_date")
    private long shiftDate;

    @ColumnInfo(name = "start_time")
    private String startTime;

    @ColumnInfo(name = "end_time")
    private String endTime;

    @ColumnInfo(name = "status")
    private String status; // DRAFT, PUBLISHED, IN_PROGRESS, CLOSED, CANCELLED

    @ColumnInfo(name = "opened_by")
    private Integer openedBy;

    @ColumnInfo(name = "opened_at")
    private Long openedAt;

    @ColumnInfo(name = "closed_by")
    private Integer closedBy;

    @ColumnInfo(name = "closed_at")
    private Long closedAt;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    // Getters and Setters
    public int getShiftId() { return shiftId; }
    public void setShiftId(int shiftId) { this.shiftId = shiftId; }

    public Integer getTemplateId() { return templateId; }
    public void setTemplateId(Integer templateId) { this.templateId = templateId; }

    public String getShiftName() { return shiftName; }
    public void setShiftName(String shiftName) { this.shiftName = shiftName; }

    public long getShiftDate() { return shiftDate; }
    public void setShiftDate(long shiftDate) { this.shiftDate = shiftDate; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getOpenedBy() { return openedBy; }
    public void setOpenedBy(Integer openedBy) { this.openedBy = openedBy; }

    public Long getOpenedAt() { return openedAt; }
    public void setOpenedAt(Long openedAt) { this.openedAt = openedAt; }

    public Integer getClosedBy() { return closedBy; }
    public void setClosedBy(Integer closedBy) { this.closedBy = closedBy; }

    public Long getClosedAt() { return closedAt; }
    public void setClosedAt(Long closedAt) { this.closedAt = closedAt; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
