package com.example.cafe_manager.data.remote;

public class SetAvailabilityRequest {
    private Integer templateId;
    private Integer dayOfWeek;
    private Boolean isAvailable;

    public SetAvailabilityRequest() {}

    public SetAvailabilityRequest(Integer templateId, Integer dayOfWeek, Boolean isAvailable) {
        this.templateId = templateId;
        this.dayOfWeek = dayOfWeek;
        this.isAvailable = isAvailable;
    }

    public Integer getTemplateId() { return templateId; }
    public void setTemplateId(Integer templateId) { this.templateId = templateId; }

    public Integer getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }
}
