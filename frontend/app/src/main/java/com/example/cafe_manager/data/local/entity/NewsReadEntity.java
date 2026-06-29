package com.example.cafe_manager.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "news_reads",
    foreignKeys = {
        @ForeignKey(
            entity = NewsPostEntity.class,
            parentColumns = "post_id",
            childColumns = "post_id",
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
        @Index(value = {"post_id", "user_id"}, unique = true),
        @Index(value = {"user_id", "readAt"})
    }
)
public class NewsReadEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "read_id")
    private int readId;

    @ColumnInfo(name = "post_id")
    private int postId;

    @ColumnInfo(name = "user_id")
    private int userId;

    @ColumnInfo(name = "readAt")
    private long readAt; // epoch millis

    public NewsReadEntity() {}

    public int getReadId() {
        return readId;
    }

    public void setReadId(int readId) {
        this.readId = readId;
    }

    public int getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
        this.postId = postId;
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
