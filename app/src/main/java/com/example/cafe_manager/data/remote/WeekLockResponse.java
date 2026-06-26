package com.example.cafe_manager.data.remote;

import com.google.gson.annotations.SerializedName;

public class WeekLockResponse {

    @SerializedName("locked")
    private boolean locked;

    @SerializedName("lockedAt")
    private Long lockedAt; // nullable

    @SerializedName("lockedBy")
    private Integer lockedBy; // nullable

    public WeekLockResponse() {}

    public WeekLockResponse(boolean locked, Long lockedAt, Integer lockedBy) {
        this.locked = locked;
        this.lockedAt = lockedAt;
        this.lockedBy = lockedBy;
    }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public Long getLockedAt() { return lockedAt; }
    public void setLockedAt(Long lockedAt) { this.lockedAt = lockedAt; }

    public Integer getLockedBy() { return lockedBy; }
    public void setLockedBy(Integer lockedBy) { this.lockedBy = lockedBy; }
}
