package com.example.cafe_manager.data.remote;

public class CreateNewsRequest {
    private String title;
    private String content;
    private String type;
    private String priority;
    private String targetType;
    private String targetRole;
    private Integer targetShiftId;
    private boolean isPinned;

    public CreateNewsRequest() {}

    public CreateNewsRequest(String title, String content, String type, String priority,
                            String targetType, String targetRole, Integer targetShiftId, boolean isPinned) {
        this.title = title;
        this.content = content;
        this.type = type;
        this.priority = priority;
        this.targetType = targetType;
        this.targetRole = targetRole;
        this.targetShiftId = targetShiftId;
        this.isPinned = isPinned;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public Integer getTargetShiftId() {
        return targetShiftId;
    }

    public void setTargetShiftId(Integer targetShiftId) {
        this.targetShiftId = targetShiftId;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public void setPinned(boolean pinned) {
        isPinned = pinned;
    }
}