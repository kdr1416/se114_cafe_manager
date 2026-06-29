package com.example.cafe_manager.data.remote;

public class SchedulingRequest {
    private Long startDate;
    private Long endDate;

    public SchedulingRequest() {}

    public Long getStartDate() {
        return startDate;
    }

    public void setStartDate(Long startDate) {
        this.startDate = startDate;
    }

    public Long getEndDate() {
        return endDate;
    }

    public void setEndDate(Long endDate) {
        this.endDate = endDate;
    }
}
