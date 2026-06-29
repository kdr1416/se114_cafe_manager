package com.example.cafe_manager.ui.common;

import com.example.cafe_manager.ui.availability.model.AvailabilitySlotUiModel;

public abstract class WeeklyCalendarItem {
    public static final int TYPE_DAY_HEADER = 0;
    public static final int TYPE_AVAILABILITY_SLOT = 1;
    public static final int TYPE_SHIFT_CARD = 2;      // reserved for future
    public static final int TYPE_SUGGESTION_CARD = 3;  // reserved for future

    public abstract int getType();

    public static class DayHeader extends WeeklyCalendarItem {
        private final String dayLabel;
        private final int dayOfWeek;
        private final boolean isLocked;

        public DayHeader(String dayLabel, int dayOfWeek, boolean isLocked) {
            this.dayLabel = dayLabel;
            this.dayOfWeek = dayOfWeek;
            this.isLocked = isLocked;
        }

        public String getDayLabel() { return dayLabel; }
        public int getDayOfWeek() { return dayOfWeek; }
        public boolean isLocked() { return isLocked; }

        @Override
        public int getType() { return TYPE_DAY_HEADER; }
    }

    public static class AvailabilitySlot extends WeeklyCalendarItem {
        private final AvailabilitySlotUiModel slot;
        private final boolean isLocked;

        public AvailabilitySlot(AvailabilitySlotUiModel slot, boolean isLocked) {
            this.slot = slot;
            this.isLocked = isLocked;
        }

        public AvailabilitySlotUiModel getSlot() { return slot; }
        public boolean isLocked() { return isLocked; }

        @Override
        public int getType() { return TYPE_AVAILABILITY_SLOT; }
    }
}
