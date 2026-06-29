package com.example.cafe_manager.data.remote;

public class CheckOutRequest {
    private Integer shiftId;
    private String notes;

    public CheckOutRequest() {}

    public Integer getShiftId() {
        return shiftId;
    }

    public void setShiftId(Integer shiftId) {
        this.shiftId = shiftId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
