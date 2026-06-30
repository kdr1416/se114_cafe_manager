package com.example.cafe_manager.util;

public final class ShiftTimeUtils {

    private ShiftTimeUtils() {}

    /**
     * Parses HH:mm format time to milliseconds since epoch for a given base date.
     */
    public static long getShiftStartMillis(long baseDate, String startTimeStr) {
        return baseDate + parseTimeToMillis(startTimeStr);
    }

    /**
     * Parses HH:mm format time to milliseconds. If the end time is less than or equal to
     * the start time, it assumes the shift crosses midnight and adds 24 hours to the end time.
     */
    public static long getShiftEndMillis(long baseDate, String startTimeStr, String endTimeStr) {
        long start = getShiftStartMillis(baseDate, startTimeStr);
        long end = baseDate + parseTimeToMillis(endTimeStr);
        if (end <= start) {
            end += 24 * 3600 * 1000L; // Add 24 hours (1 day)
        }
        return end;
    }

    private static long parseTimeToMillis(String timeStr) {
        if (timeStr == null || !timeStr.contains(":")) {
            return 0;
        }
        try {
            String[] parts = timeStr.split(":");
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            return (hours * 3600L + minutes * 60L) * 1000L;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Checks whether two shifts overlap.
     * Uses timestamp-based comparison: newStartAt < oldEndAt && newEndAt > oldStartAt
     */
    public static boolean checkOverlap(
            long dateA, String startA, String endA,
            long dateB, String startB, String endB
    ) {
        long startMillisA = getShiftStartMillis(dateA, startA);
        long endMillisA = getShiftEndMillis(dateA, startA, endA);

        long startMillisB = getShiftStartMillis(dateB, startB);
        long endMillisB = getShiftEndMillis(dateB, startB, endB);

        return startMillisA < endMillisB && endMillisA > startMillisB;
    }
}
