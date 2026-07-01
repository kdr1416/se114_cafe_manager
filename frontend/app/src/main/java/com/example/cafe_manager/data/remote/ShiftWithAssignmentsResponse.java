package com.example.cafe_manager.data.remote;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ShiftWithAssignmentsResponse {
    @SerializedName("shiftId")
    private Integer shiftId;

    @SerializedName("shiftName")
    private String shiftName;

    @SerializedName("shiftDate")
    private Long shiftDate;

    @SerializedName("startTime")
    private String startTime;

    @SerializedName("endTime")
    private String endTime;

    @SerializedName("status")
    private String status;

    @SerializedName("templateId")
    private Integer templateId;

    @SerializedName("minStaff")
    private Integer minStaff;

    @SerializedName("assignedCount")
    private Integer assignedCount;

    @SerializedName("assignedStaff")
    private List<AssignedStaffDto> assignedStaff;

    public ShiftWithAssignmentsResponse() {}

    public Integer getShiftId() { return shiftId; }
    public void setShiftId(Integer shiftId) { this.shiftId = shiftId; }

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

    public Integer getTemplateId() { return templateId; }
    public void setTemplateId(Integer templateId) { this.templateId = templateId; }

    public Integer getMinStaff() { return minStaff; }
    public void setMinStaff(Integer minStaff) { this.minStaff = minStaff; }

    public Integer getAssignedCount() { return assignedCount; }
    public void setAssignedCount(Integer assignedCount) { this.assignedCount = assignedCount; }

    public List<AssignedStaffDto> getAssignedStaff() { return assignedStaff; }
    public void setAssignedStaff(List<AssignedStaffDto> assignedStaff) { this.assignedStaff = assignedStaff; }

    public static class AssignedStaffDto {
        @SerializedName("userId")
        private Integer userId;

        @SerializedName("fullName")
        private String fullName;

        @SerializedName("role")
        private String role;

        @SerializedName("confirmed")
        private Boolean confirmed;

        public AssignedStaffDto() {}

        public Integer getUserId() { return userId; }
        public void setUserId(Integer userId) { this.userId = userId; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public Boolean getConfirmed() { return confirmed; }
        public void setConfirmed(Boolean confirmed) { this.confirmed = confirmed; }
    }
}
