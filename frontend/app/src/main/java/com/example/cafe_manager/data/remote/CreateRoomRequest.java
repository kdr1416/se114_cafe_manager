package com.example.cafe_manager.data.remote;

import com.google.gson.annotations.SerializedName;

public class CreateRoomRequest {
    @SerializedName("roomName")
    private String roomName;

    @SerializedName("targetRole")
    private String targetRole;

    public CreateRoomRequest() {}

    public CreateRoomRequest(String roomName, String targetRole) {
        this.roomName = roomName;
        this.targetRole = targetRole;
    }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }
}
