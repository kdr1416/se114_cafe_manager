package com.example.cafe_manager.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "news_posts",
    foreignKeys = @ForeignKey(
        entity = UserEntity.class,
        parentColumns = "user_id",
        childColumns = "created_by_user_id",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {
        @Index(value = {"createdAt"}),
        @Index(value = {"is_deleted", "createdAt"}),
        @Index(value = {"target_type", "target_role"}),
        @Index(value = {"created_by_user_id"})
    }
)
public class NewsPostEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "post_id")
    private int postId;

    @ColumnInfo(name = "title")
    private String title; // length <= 100 in validation

    @ColumnInfo(name = "content")
    private String content; // length <= 2000 in validation

    @ColumnInfo(name = "type")
    private String type; // GENERAL, MEETING, SHIFT, RULE, URGENT, PROMOTION, STOCK

    @ColumnInfo(name = "priority")
    private String priority; // NORMAL, IMPORTANT, URGENT

    @ColumnInfo(name = "target_type")
    private String targetType; // ALL, ROLE, SHIFT

    @ColumnInfo(name = "target_role")
    private String targetRole; // required if targetType = ROLE

    @ColumnInfo(name = "target_shift_id")
    private Integer targetShiftId; // required if targetType = SHIFT, nullable

    @ColumnInfo(name = "created_by_user_id")
    private int createdByUserId;

    @ColumnInfo(name = "createdAt")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private Long updatedAt; // nullable

    @ColumnInfo(name = "is_pinned")
    private boolean isPinned = false;

    @ColumnInfo(name = "is_deleted")
    private boolean isDeleted = false;

    public NewsPostEntity() {}

    public int getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public Integer getTargetShiftId() {
        return targetShiftId;
    }

    public void setTargetShiftId(Integer targetShiftId) {
        this.targetShiftId = targetShiftId;
    }

    public int getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(int createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean getIsPinned() {
        return isPinned;
    }

    public void setIsPinned(boolean isPinned) {
        this.isPinned = isPinned;
    }

    public boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    // Transient field - not persisted in Room DB, used for read status from server
    private boolean isRead;

    public boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(boolean isRead) {
        this.isRead = isRead;
    }
}
