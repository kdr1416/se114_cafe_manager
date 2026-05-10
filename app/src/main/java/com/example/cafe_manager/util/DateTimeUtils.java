package com.example.cafe_manager.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateTimeUtils {

    private static final String DEFAULT_PATTERN = "dd/MM/yyyy HH:mm";

    public static String formatDateTime(long timestamp) {
        if (timestamp <= 0) {
            return "";
        }

        SimpleDateFormat sdf = new SimpleDateFormat(DEFAULT_PATTERN, Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static long now() {
        return System.currentTimeMillis();
    }
}