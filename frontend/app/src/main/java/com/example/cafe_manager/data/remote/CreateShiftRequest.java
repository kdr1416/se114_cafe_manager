package com.example.cafe_manager.data.remote;

import java.util.List;

public class CreateShiftRequest {
    private Integer templateId;
    private String shiftName;
    private Long shiftDate;
    private String startTime;
    private String endTime;

    public CreateShiftRequest() {}

    public Integer getTemplateId() { return templateId; }
    public void setTemplateId(Integer templateId) { this.templateId = templateId; }

    public String getShiftName() { return shiftName; }
    public void setShiftName(String shiftName) { this.shiftName = shiftName; }

    public Long getShiftDate() { return shiftDate; }
    public void setShiftDate(Long shiftDate) { this.shiftDate = shiftDate; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}
