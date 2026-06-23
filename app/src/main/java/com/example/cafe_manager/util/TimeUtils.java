package com.example.cafe_manager.util;

import android.text.format.DateUtils;

import java.util.Date;

public class TimeUtils {

    /**
     * Format a timestamp into a human-readable relative time string.
     * Examples:
     * - "Vừa xong" (less than 1 minute)
     * - "2 phút trước"
     * - "1 giờ trước"
     * - "Hôm qua, 14:30"
     * - "22/06/2026"
     *
     * @param timestampMillis The timestamp in milliseconds
     * @return Formatted relative time string in Vietnamese
     */
    public static String formatRelativeTime(long timestampMillis) {
        long now = System.currentTimeMillis();
        long diff = now - timestampMillis;

        // Less than 1 minute
        if (diff < DateUtils.MINUTE_IN_MILLIS) {
            return "Vừa xong";
        }
        // Less than 1 hour
        else if (diff < DateUtils.HOUR_IN_MILLIS) {
            int minutes = (int) (diff / DateUtils.MINUTE_IN_MILLIS);
            return minutes + " phút trước";
        }
        // Less than 24 hours
        else if (diff < DateUtils.DAY_IN_MILLIS) {
            int hours = (int) (diff / DateUtils.HOUR_IN_MILLIS);
            return hours + " giờ trước";
        }
        // Less than 48 hours (yesterday)
        else if (diff < 2 * DateUtils.DAY_IN_MILLIS) {
            java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
            return "Hôm qua, " + timeFormat.format(new Date(timestampMillis));
        }
        // Less than 7 days
        else if (diff < 7 * DateUtils.DAY_IN_MILLIS) {
            java.text.SimpleDateFormat fullFormat = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
            return fullFormat.format(new Date(timestampMillis));
        }
        // Older than 7 days
        else {
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            return dateFormat.format(new Date(timestampMillis));
        }
    }

    /**
     * Format a timestamp into a short time string (HH:mm).
     *
     * @param timestampMillis The timestamp in milliseconds
     * @return Time formatted as HH:mm
     */
    public static String formatTimeOnly(long timestampMillis) {
        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        return timeFormat.format(new Date(timestampMillis));
    }

    /**
     * Format a timestamp into a date-only string (dd/MM/yyyy).
     *
     * @param timestampMillis The timestamp in milliseconds
     * @return Date formatted as dd/MM/yyyy
     */
    public static String formatDateOnly(long timestampMillis) {
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
        return dateFormat.format(new Date(timestampMillis));
    }
}
