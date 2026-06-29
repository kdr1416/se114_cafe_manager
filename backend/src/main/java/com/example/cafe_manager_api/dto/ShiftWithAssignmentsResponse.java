package com.example.cafe_manager_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShiftWithAssignmentsResponse {
    private Integer shiftId;
    private String shiftName;
    private Long shiftDate;
    private String startTime;
    private String endTime;
    private String status;
    private Integer templateId;
    private Integer minStaff;
    private Integer assignedCount;
    private List<AssignedStaffDto> assignedStaff;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignedStaffDto {
        private Integer userId;
        private String fullName;
        private String role;
        private Boolean confirmed;
    }
}
