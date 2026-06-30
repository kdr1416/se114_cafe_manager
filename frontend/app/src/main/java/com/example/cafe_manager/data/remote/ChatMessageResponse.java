package com.example.cafe_manager.data.remote;

import com.google.gson.annotations.SerializedName;

public class ChatMessageResponse {
    @SerializedName("messageId")
    private Integer messageId;

    @SerializedName("roomId")
    private Integer roomId;

    @SerializedName("senderId")
    private Integer senderId;

    @SerializedName("senderName")
    private String senderName;

    @SerializedName("content")
    private String content;

    @SerializedName("createdAt")
    private Long createdAt;

    @SerializedName("isRead")
    private Boolean isRead;

    @SerializedName("isDeleted")
    private Boolean isDeleted;

    public ChatMessageResponse() {}

    public Integer getMessageId() { return messageId; }
    public void setMessageId(Integer messageId) { this.messageId = messageId; }

    public Integer getRoomId() { return roomId; }
    public void setRoomId(Integer roomId) { this.roomId = roomId; }

    public Integer getSenderId() { return senderId; }
    public void setSenderId(Integer senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }
}
