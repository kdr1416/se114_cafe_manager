package com.example.cafe_manager.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class DateTimeUtils {
    private static final String DATE_TIME_PATTERN = "dd/MM/yyyy HH:mm";
    private static final String DATE_PATTERN = "dd/MM/yyyy";

    private DateTimeUtils() {
    }

    public static long now() {
        return System.currentTimeMillis();
    }

    public static String formatDateTime(long timestamp) {
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_TIME_PATTERN, Locale.getDefault());
        return formatter.format(new Date(timestamp));
    }

    public static String formatDate(long timestamp) {
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_PATTERN, Locale.getDefault());
        return formatter.format(new Date(timestamp));
    }
}
