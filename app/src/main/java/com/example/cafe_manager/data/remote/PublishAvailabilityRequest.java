package com.example.cafe_manager.data.remote;

import com.google.gson.annotations.SerializedName;

public class PublishAvailabilityRequest {

    @SerializedName("templateId")
    private Integer templateId;

    @SerializedName("dayOfWeek")
    private Integer dayOfWeek;

    @SerializedName("isAvailable")
    private Boolean isAvailable;

    @SerializedName("scope")
    private String scope; // "THIS_WEEK" or "UNTIL_DATE"

    @SerializedName("untilDate")
    private Long untilDate; // nullable

    public PublishAvailabilityRequest() {}

    public PublishAvailabilityRequest(Integer templateId, Integer dayOfWeek, Boolean isAvailable,
                                      String scope, Long untilDate) {
        this.templateId = templateId;
        this.dayOfWeek = dayOfWeek;
        this.isAvailable = isAvailable;
        this.scope = scope;
        this.untilDate = untilDate;
    }

    public Integer getTemplateId() { return templateId; }
    public void setTemplateId(Integer templateId) { this.templateId = templateId; }

    public Integer getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public Long getUntilDate() { return untilDate; }
    public void setUntilDate(Long untilDate) { this.untilDate = untilDate; }
}
