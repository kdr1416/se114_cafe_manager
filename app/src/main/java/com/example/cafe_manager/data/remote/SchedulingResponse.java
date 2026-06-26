package com.example.cafe_manager.data.remote;

import java.util.List;

public class SchedulingResponse {
    private Long runId;
    private Long startDate;
    private Long endDate;
    private String status;
    private Long createdAt;
    private Long appliedAt;
    private List<ShiftSuggestion> suggestions;
    private Integer totalShifts;
    private Integer fulfilledShifts;
    private Integer missingShifts;

    public SchedulingResponse() {}

    public Long getRunId() {
        return runId;
    }

    public void setRunId(Long runId) {
        this.runId = runId;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(Long appliedAt) {
        this.appliedAt = appliedAt;
    }

    public List<ShiftSuggestion> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<ShiftSuggestion> suggestions) {
        this.suggestions = suggestions;
    }

    public Integer getTotalShifts() {
        return totalShifts;
    }

    public void setTotalShifts(Integer totalShifts) {
        this.totalShifts = totalShifts;
    }

    public Integer getFulfilledShifts() {
        return fulfilledShifts;
    }

    public void setFulfilledShifts(Integer fulfilledShifts) {
        this.fulfilledShifts = fulfilledShifts;
    }

    public Integer getMissingShifts() {
        return missingShifts;
    }

    public void setMissingShifts(Integer missingShifts) {
        this.missingShifts = missingShifts;
    }
}
