package com.example.cafe_manager.util;

import java.util.Calendar;

/**
 * Helper tính khoảng thời gian [fromMs, toMs] cho History/Dashboard filter.
 */
public final class DateRange {

    public enum Period { TODAY, WEEK, MONTH, ALL }

    private DateRange() {}

    /** Trả về [from, to] millis cho period. */
    public static long[] compute(Period period) {
        long now = System.currentTimeMillis();

        if (period == Period.ALL) {
            return new long[]{0L, now};
        }

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        switch (period) {
            case TODAY:
                break;
            case WEEK:
                cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                break;
            case MONTH:
                cal.set(Calendar.DAY_OF_MONTH, 1);
                break;
            default:
        }
        return new long[]{cal.getTimeInMillis(), now};
    }

    public static String displayName(Period p) {
        if (p == null) return "";
        switch (p) {
            case TODAY: return "Hôm nay";
            case WEEK:  return "Tuần này";
            case MONTH: return "Tháng này";
            case ALL:   return "Tất cả";
            default:    return p.name();
        }
    }
}
