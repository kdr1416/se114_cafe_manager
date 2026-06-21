package com.example.cafe_manager.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "shift_assignments")
public class ShiftAssignmentEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "assignment_id")
    private int assignmentId;

    @ColumnInfo(name = "shift_id")
    private int shiftId;

    @ColumnInfo(name = "user_id")
    private int userId;

    @ColumnInfo(name = "role")
    private String role;

    @ColumnInfo(name = "assigned_by")
    private Integer assignedBy;

    @ColumnInfo(name = "confirmed")
    private boolean confirmed;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    // Getters and Setters
    public int getAssignmentId() { return assignmentId; }
    public void setAssignmentId(int assignmentId) { this.assignmentId = assignmentId; }

    public int getShiftId() { return shiftId; }
    public void setShiftId(int shiftId) { this.shiftId = shiftId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Integer getAssignedBy() { return assignedBy; }
    public void setAssignedBy(Integer assignedBy) { this.assignedBy = assignedBy; }

    public boolean isConfirmed() { return confirmed; }
    public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
