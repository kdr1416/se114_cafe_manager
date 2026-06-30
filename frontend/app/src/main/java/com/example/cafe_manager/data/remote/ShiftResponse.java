package com.example.cafe_manager.data.remote;

public class ShiftResponse {
    private int shiftId;
    private Integer templateId;
    private String shiftName;
    private Long shiftDate;
    private String startTime;
    private String endTime;
    private String status;
    private Integer openedBy;
    private Long openedAt;
    private Integer closedBy;
    private Long closedAt;
    private Double openingCash;
    private Double closingCash;
    private Long createdAt;

    public ShiftResponse() {}

    public int getShiftId() { return shiftId; }
    public void setShiftId(int shiftId) { this.shiftId = shiftId; }

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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getOpenedBy() { return openedBy; }
    public void setOpenedBy(Integer openedBy) { this.openedBy = openedBy; }

    public Long getOpenedAt() { return openedAt; }
    public void setOpenedAt(Long openedAt) { this.openedAt = openedAt; }

    public Integer getClosedBy() { return closedBy; }
    public void setClosedBy(Integer closedBy) { this.closedBy = closedBy; }

    public Long getClosedAt() { return closedAt; }
    public void setClosedAt(Long closedAt) { this.closedAt = closedAt; }

    public Double getOpeningCash() { return openingCash; }
    public void setOpeningCash(Double openingCash) { this.openingCash = openingCash; }

    public Double getClosingCash() { return closingCash; }
    public void setClosingCash(Double closingCash) { this.closingCash = closingCash; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
}
