package com.example.cafe_manager.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "chat_messages",
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
            childColumns = "sender_id",
            onDelete = ForeignKey.CASCADE
        )
    },
    indices = {
        @Index(value = "room_id"),
        @Index(value = "sender_id"),
        @Index(value = {"room_id", "created_at"})
    }
)
public class ChatMessageEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "message_id")
    private int messageId;

    @ColumnInfo(name = "room_id")
    private int roomId;

    @ColumnInfo(name = "sender_id")
    private int senderId;

    @ColumnInfo(name = "content")
    private String content;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "is_deleted")
    private boolean isDeleted = false;

    public ChatMessageEntity() {}

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }
}
