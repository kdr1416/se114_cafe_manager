package com.example.cafe_manager.data.remote;

public class UpdateUserStatusRequest {
    private boolean isActive;

    public UpdateUserStatusRequest() {}

    public UpdateUserStatusRequest(boolean isActive) {
        this.isActive = isActive;
    }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
