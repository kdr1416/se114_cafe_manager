package com.example.cafe_manager.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "shift_assignments",
    indices = {@Index(value = {"shift_id", "user_id"}, unique = true)}
)
public class ShiftAssignmentEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "assignment_id")
    private int assignmentId;

    @ColumnInfo(name = "shift_id")
    private int shiftId;

    @ColumnInfo(name = "user_id")
    private int userId;

    @ColumnInfo(name = "role")
    private String role; // Vai trò mong muốn trong ca (ADMIN, MANAGER, STAFF)

    @ColumnInfo(name = "assigned_by")
    private int assignedBy;

    @ColumnInfo(name = "confirmed")
    private boolean confirmed; // Nhân viên đã xác nhận làm ca này chưa

    @ColumnInfo(name = "created_at")
    private long createdAt;

    public ShiftAssignmentEntity() {}

    public int getAssignmentId() { return assignmentId; }
    public void setAssignmentId(int assignmentId) { this.assignmentId = assignmentId; }

    public int getShiftId() { return shiftId; }
    public void setShiftId(int shiftId) { this.shiftId = shiftId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public int getAssignedBy() { return assignedBy; }
    public void setAssignedBy(int assignedBy) { this.assignedBy = assignedBy; }

    public boolean isConfirmed() { return confirmed; }
    public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
