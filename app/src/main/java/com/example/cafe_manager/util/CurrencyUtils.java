package com.example.cafe_manager.util;

import java.text.NumberFormat;
import java.util.Locale;

public final class CurrencyUtils {
    private static final Locale VIETNAM_LOCALE = new Locale("vi", "VN");

    private CurrencyUtils() {
    }

    public static String formatVnd(double amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(VIETNAM_LOCALE);
        return formatter.format(amount);
    }

    public static double normalizeAmount(double amount) {
        return Math.max(amount, 0);
    }
}
