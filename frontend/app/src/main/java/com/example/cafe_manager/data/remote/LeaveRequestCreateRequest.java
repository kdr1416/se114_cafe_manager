package com.example.cafe_manager.data.remote;

public class LeaveRequestCreateRequest {
    private Long startAt;
    private Long endAt;
    private String reason;

    public LeaveRequestCreateRequest() {}

    public LeaveRequestCreateRequest(Long startAt, Long endAt, String reason) {
        this.startAt = startAt;
        this.endAt = endAt;
        this.reason = reason;
    }

    public Long getStartAt() {
        return startAt;
    }

    public void setStartAt(Long startAt) {
        this.startAt = startAt;
    }

    public Long getEndAt() {
        return endAt;
    }

    public void setEndAt(Long endAt) {
        this.endAt = endAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
