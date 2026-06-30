package com.example.cafe_manager.data.remote;

public class UpdateShiftRequest {
    private String shiftName;
    private String startTime;
    private String endTime;

    public UpdateShiftRequest() {}

    public String getShiftName() { return shiftName; }
    public void setShiftName(String shiftName) { this.shiftName = shiftName; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}
