package com.example.cafe_manager.data.remote;

import com.google.gson.annotations.SerializedName;

public class ChatRoomResponse {
    @SerializedName("roomId")
    private Integer roomId;

    @SerializedName("roomName")
    private String roomName;

    @SerializedName("roomType")
    private String roomType;

    @SerializedName("shiftId")
    private Integer shiftId;

    @SerializedName("lastMessage")
    private String lastMessage;

    @SerializedName("lastMessageAt")
    private Long lastMessageAt;

    @SerializedName("unreadCount")
    private Integer unreadCount;

    @SerializedName("participantCount")
    private Integer participantCount;

    @SerializedName("isActive")
    private Boolean isActive;

    public ChatRoomResponse() {}

    public Integer getRoomId() { return roomId; }
    public void setRoomId(Integer roomId) { this.roomId = roomId; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public Integer getShiftId() { return shiftId; }
    public void setShiftId(Integer shiftId) { this.shiftId = shiftId; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public Long getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(Long lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public Integer getUnreadCount() { return unreadCount; }
    public void setUnreadCount(Integer unreadCount) { this.unreadCount = unreadCount; }

    public Integer getParticipantCount() { return participantCount; }
    public void setParticipantCount(Integer participantCount) { this.participantCount = participantCount; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
