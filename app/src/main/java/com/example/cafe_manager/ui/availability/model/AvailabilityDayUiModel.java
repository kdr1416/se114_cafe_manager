package com.example.cafe_manager.ui.availability.model;

import java.util.List;

public class AvailabilityDayUiModel {
    private int dayOfWeek;
    private String dayLabel;
    private List<AvailabilitySlotUiModel> slots;

    public AvailabilityDayUiModel() {}

    public AvailabilityDayUiModel(int dayOfWeek, String dayLabel, List<AvailabilitySlotUiModel> slots) {
        this.dayOfWeek = dayOfWeek;
        this.dayLabel = dayLabel;
        this.slots = slots;
    }

    public int getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public String getDayLabel() { return dayLabel; }
    public void setDayLabel(String dayLabel) { this.dayLabel = dayLabel; }

    public List<AvailabilitySlotUiModel> getSlots() { return slots; }
    public void setSlots(List<AvailabilitySlotUiModel> slots) { this.slots = slots; }
}
