package com.example.cafe_manager.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class WeekNavigationHelper {

    private static final TimeZone TZ = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");

    private WeekNavigationHelper() {} // no instantiation

    /** Returns epoch millis of Monday 00:00:00.000 Asia/Ho_Chi_Minh of current week. */
    public static long getCurrentWeekStart() {
        return getWeekStart(System.currentTimeMillis());
    }

    /** Returns Monday 00:00:00.000 for the week containing epochMillis. */
    public static long getWeekStart(long epochMillis) {
        Calendar cal = Calendar.getInstance(TZ);
        cal.setTimeInMillis(epochMillis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // Calendar.DAY_OF_WEEK: SUNDAY=1, MONDAY=2, ..., SATURDAY=7
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        int daysToSubtract = (dow == Calendar.SUNDAY) ? 6 : (dow - Calendar.MONDAY);
        cal.add(Calendar.DAY_OF_YEAR, -daysToSubtract);
        return cal.getTimeInMillis();
    }

    /** Returns weekStart + (weeks * 7 days). */
    public static long addWeeks(long weekStart, int weeks) {
        Calendar cal = Calendar.getInstance(TZ);
        cal.setTimeInMillis(weekStart);
        cal.add(Calendar.WEEK_OF_YEAR, weeks);
        return cal.getTimeInMillis();
    }

    /** Returns "01/07 – 07/07/2026" format. */
    public static String formatWeekRange(long weekStart) {
        Calendar cal = Calendar.getInstance(TZ);
        SimpleDateFormat dfShort = new SimpleDateFormat("dd/MM", new Locale("vi", "VN"));
        dfShort.setTimeZone(TZ);
        SimpleDateFormat dfFull = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"));
        dfFull.setTimeZone(TZ);

        cal.setTimeInMillis(weekStart);
        String start = dfShort.format(cal.getTime());

        cal.add(Calendar.DAY_OF_YEAR, 6); // Sunday
        String end = dfFull.format(cal.getTime());

        return start + " – " + end;
    }

    /** Returns "Thứ 2, 01/07" format for a given epoch millis. */
    public static String formatDayLabel(long dayEpoch) {
        Calendar cal = Calendar.getInstance(TZ);
        cal.setTimeInMillis(dayEpoch);

        int dow = getIsoDayOfWeek(dayEpoch);
        String dayName = getDayName(dow);

        SimpleDateFormat df = new SimpleDateFormat("dd/MM", new Locale("vi", "VN"));
        df.setTimeZone(TZ);

        return dayName + ", " + df.format(cal.getTime());
    }

    /** Returns 1=Mon ... 7=Sun using Asia/Ho_Chi_Minh. */
    public static int getIsoDayOfWeek(long epochMillis) {
        Calendar cal = Calendar.getInstance(TZ);
        cal.setTimeInMillis(epochMillis);
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        // Convert: SUNDAY(1)->7, MONDAY(2)->1, ..., SATURDAY(7)->6
        return (dow == Calendar.SUNDAY) ? 7 : (dow - 1);
    }

    private static String getDayName(int isoDayOfWeek) {
        switch (isoDayOfWeek) {
            case 1: return "Thứ 2";
            case 2: return "Thứ 3";
            case 3: return "Thứ 4";
            case 4: return "Thứ 5";
            case 5: return "Thứ 6";
            case 6: return "Thứ 7";
            case 7: return "Chủ nhật";
            default: return "";
        }
    }
}
