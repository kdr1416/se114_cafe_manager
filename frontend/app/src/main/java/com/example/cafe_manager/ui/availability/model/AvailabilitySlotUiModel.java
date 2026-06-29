package com.example.cafe_manager.ui.availability.model;

public class AvailabilitySlotUiModel {
    private int templateId;
    private int dayOfWeek;
    private String dayLabel;
    private String shiftName;
    private String startTime;
    private String endTime;
    private boolean selected;
    private boolean originalSelected;
    private Integer availabilityId;
    private Long effectiveFromDate;
    private Long effectiveToDate;

    public AvailabilitySlotUiModel() {}

    public AvailabilitySlotUiModel(int templateId, int dayOfWeek, String dayLabel, String shiftName,
                                   String startTime, String endTime, boolean selected, boolean originalSelected,
                                   Integer availabilityId, Long effectiveFromDate, Long effectiveToDate) {
        this.templateId = templateId;
        this.dayOfWeek = dayOfWeek;
        this.dayLabel = dayLabel;
        this.shiftName = shiftName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.selected = selected;
        this.originalSelected = originalSelected;
        this.availabilityId = availabilityId;
        this.effectiveFromDate = effectiveFromDate;
        this.effectiveToDate = effectiveToDate;
    }

    public int getTemplateId() { return templateId; }
    public void setTemplateId(int templateId) { this.templateId = templateId; }

    public int getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public String getDayLabel() { return dayLabel; }
    public void setDayLabel(String dayLabel) { this.dayLabel = dayLabel; }

    public String getShiftName() { return shiftName; }
    public void setShiftName(String shiftName) { this.shiftName = shiftName; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }

    public boolean isOriginalSelected() { return originalSelected; }
    public void setOriginalSelected(boolean originalSelected) { this.originalSelected = originalSelected; }

    public Integer getAvailabilityId() { return availabilityId; }
    public void setAvailabilityId(Integer availabilityId) { this.availabilityId = availabilityId; }

    public Long getEffectiveFromDate() { return effectiveFromDate; }
    public void setEffectiveFromDate(Long effectiveFromDate) { this.effectiveFromDate = effectiveFromDate; }

    public Long getEffectiveToDate() { return effectiveToDate; }
    public void setEffectiveToDate(Long effectiveToDate) { this.effectiveToDate = effectiveToDate; }
}
