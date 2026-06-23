package com.example.cafe_manager.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "chat_participants",
    foreignKeys = {
        @ForeignKey(
            entity = ChatRoomEntity.class,
            parentColumns = "room_id",
            childColumns = "room_id",
            onDelete = ForeignKey.CASCADE
        ),
        @ForeignKey(
            entity = UserEntity.class,
            parentColumns = "user_id",
            childColumns = "user_id",
            onDelete = ForeignKey.CASCADE
        )
    },
    indices = {
        @Index(value = {"room_id", "user_id"}, unique = true),
        @Index(value = "user_id")
    }
)
public class ChatParticipantEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "participant_id")
    private int participantId;

    @ColumnInfo(name = "room_id")
    private int roomId;

    @ColumnInfo(name = "user_id")
    private int userId;

    @ColumnInfo(name = "joined_at")
    private long joinedAt;

    @ColumnInfo(name = "left_at")
    private Long leftAt; // nullable, for tracking when user left

    @ColumnInfo(name = "role_in_room")
    private String roleInRoom; // OWNER, MODERATOR, MEMBER

    public ChatParticipantEntity() {}

    public int getParticipantId() {
        return participantId;
    }

    public void setParticipantId(int participantId) {
        this.participantId = participantId;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public long getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(long joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Long getLeftAt() {
        return leftAt;
    }

    public void setLeftAt(Long leftAt) {
        this.leftAt = leftAt;
    }

    public String getRoleInRoom() {
        return roleInRoom;
    }

    public void setRoleInRoom(String roleInRoom) {
        this.roleInRoom = roleInRoom;
    }
}
