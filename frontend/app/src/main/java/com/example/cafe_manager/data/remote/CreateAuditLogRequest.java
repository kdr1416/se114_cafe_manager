package com.example.cafe_manager.data.remote;

public class CreateAuditLogRequest {
    private String action;
    private String targetType;
    private String targetId;
    private String description;

    public CreateAuditLogRequest() {}

    public CreateAuditLogRequest(String action, String targetType, String targetId, String description) {
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.description = description;
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
