package com.example.cafe_manager.data.remote;

public class AvailabilityResponse {
    private int availabilityId;
    private int userId;
    private int templateId;
    private int dayOfWeek;
    private Long effectiveFromDate;
    private Long effectiveToDate;
    private boolean isAvailable;
    private long createdAt;
    private Long updatedAt;
    private String status;
    private Long weekStart;
    private Long publishedUntil;

    public AvailabilityResponse() {}

    public AvailabilityResponse(int availabilityId, int userId, int templateId, int dayOfWeek,
                                Long effectiveFromDate, Long effectiveToDate, boolean isAvailable,
                                long createdAt, Long updatedAt) {
        this.availabilityId = availabilityId;
        this.userId = userId;
        this.templateId = templateId;
        this.dayOfWeek = dayOfWeek;
        this.effectiveFromDate = effectiveFromDate;
        this.effectiveToDate = effectiveToDate;
        this.isAvailable = isAvailable;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getAvailabilityId() { return availabilityId; }
    public void setAvailabilityId(int availabilityId) { this.availabilityId = availabilityId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getTemplateId() { return templateId; }
    public void setTemplateId(int templateId) { this.templateId = templateId; }

    public int getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public Long getEffectiveFromDate() { return effectiveFromDate; }
    public void setEffectiveFromDate(Long effectiveFromDate) { this.effectiveFromDate = effectiveFromDate; }

    public Long getEffectiveToDate() { return effectiveToDate; }
    public void setEffectiveToDate(Long effectiveToDate) { this.effectiveToDate = effectiveToDate; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getWeekStart() { return weekStart; }
    public void setWeekStart(Long weekStart) { this.weekStart = weekStart; }

    public Long getPublishedUntil() { return publishedUntil; }
    public void setPublishedUntil(Long publishedUntil) { this.publishedUntil = publishedUntil; }
}
