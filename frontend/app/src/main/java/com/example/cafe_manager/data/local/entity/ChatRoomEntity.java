package com.example.cafe_manager.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "chat_rooms",
    indices = {
        @Index(value = "shift_id"),
        @Index(value = "target_role")
    }
)
public class ChatRoomEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "room_id")
    private int roomId;

    @ColumnInfo(name = "room_name")
    private String roomName;

    @ColumnInfo(name = "room_type") // "SHIFT", "ROLE", "DIRECT", "GROUP"
    private String roomType;

    @ColumnInfo(name = "shift_id")
    private Integer shiftId; // nullable, for SHIFT type

    @ColumnInfo(name = "target_role")
    private String targetRole; // nullable, for ROLE type

    @ColumnInfo(name = "created_by")
    private int createdBy; // user_id of creator

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    @ColumnInfo(name = "is_active")
    private boolean isActive = true;

    public ChatRoomEntity() {}

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public Integer getShiftId() {
        return shiftId;
    }

    public void setShiftId(Integer shiftId) {
        this.shiftId = shiftId;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }
}
