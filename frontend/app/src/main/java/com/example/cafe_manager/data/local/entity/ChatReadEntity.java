package com.example.cafe_manager.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "chat_reads",
    foreignKeys = {
        @ForeignKey(
            entity = ChatMessageEntity.class,
            parentColumns = "message_id",
            childColumns = "message_id",
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
        @Index(value = {"message_id", "user_id"}, unique = true),
        @Index(value = "user_id")
    }
)
public class ChatReadEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "read_id")
    private int readId;

    @ColumnInfo(name = "message_id")
    private int messageId;

    @ColumnInfo(name = "user_id")
    private int userId;

    @ColumnInfo(name = "read_at")
    private long readAt;

    public ChatReadEntity() {}

    public int getReadId() {
        return readId;
    }

    public void setReadId(int readId) {
        this.readId = readId;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public long getReadAt() {
        return readAt;
    }

    public void setReadAt(long readAt) {
        this.readAt = readAt;
    }
}
