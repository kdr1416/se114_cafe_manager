package com.example.cafe_manager.util;

public final class Constants {
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_MANAGER = "MANAGER";
    public static final String ROLE_STAFF = "STAFF";

    public static final String TABLE_EMPTY = "AVAILABLE";
    public static final String TABLE_OCCUPIED = "OCCUPIED";

    public static final String ORDER_OPEN = "OPEN";
    public static final String ORDER_CONFIRMED = "CONFIRMED";
    public static final String ORDER_PAID = "PAID";
    public static final String ORDER_CANCELLED = "CANCELLED";

    public static final String PAYMENT_CASH = "CASH";
    public static final String PAYMENT_BANKING = "BANKING";
    public static final String PAYMENT_MOMO = "MOMO";

    public static final String PAYMENT_SUCCESS = "SUCCESS";
    public static final String PAYMENT_FAILED = "FAILED";

    public static final String CATEGORY_ACTIVE = "ACTIVE";
    public static final String CATEGORY_INACTIVE = "INACTIVE";

    public static final String PRODUCT_ACTIVE = "ACTIVE";
    public static final String PRODUCT_INACTIVE = "INACTIVE";

    public static final String PROMO_CASH = "CASH";
    public static final String PROMO_PERCENT = "PERCENT";

    public static final String ICON_COFFEE = "COFFEE";
    public static final String ICON_TEA = "TEA";
    public static final String ICON_FOOD = "FOOD";
    public static final String ICON_DESSERT = "DESSERT";
    public static final String ICON_OTHER = "OTHER";

    // ── Role constants ──────────────────────────────────────────

    // ── Audit log action constants ──────────────────────────────
    public static final String ACTION_LOGIN = "LOGIN";
    public static final String ACTION_LOGOUT = "LOGOUT";
    public static final String ACTION_CREATE_USER = "CREATE_USER";
    public static final String ACTION_UPDATE_USER = "UPDATE_USER";
    public static final String ACTION_LOCK_USER = "LOCK_USER";
    public static final String ACTION_RESET_PASSWORD = "RESET_PASSWORD";
    // ── Shift status constants ──────────────────────────────────
    public static final String SHIFT_DRAFT = "DRAFT";
    public static final String SHIFT_PUBLISHED = "PUBLISHED";
    public static final String SHIFT_IN_PROGRESS = "IN_PROGRESS";
    public static final String SHIFT_CLOSED = "CLOSED";
    public static final String SHIFT_CANCELLED = "CANCELLED";

    public static final String CASH_SESSION_OPEN = "OPEN";
    public static final String CASH_SESSION_CLOSED = "CLOSED";

    // ── Attendance status ───────────────────────────────────────
    public static final String ATTENDANCE_ABSENT = "ABSENT";
    public static final String ATTENDANCE_CHECKED_IN = "CHECKED_IN";
    public static final String ATTENDANCE_COMPLETED = "COMPLETED";
    public static final String ATTENDANCE_LATE = "LATE";
    public static final String ATTENDANCE_EARLY_LEAVE = "EARLY_LEAVE";

    // ── News Post Type constants ─────────────────────────────────
    public static final String NEWS_TYPE_GENERAL = "GENERAL";
    public static final String NEWS_TYPE_MEETING = "MEETING";
    public static final String NEWS_TYPE_SHIFT = "SHIFT";
    public static final String NEWS_TYPE_RULE = "RULE";
    public static final String NEWS_TYPE_URGENT = "URGENT";
    public static final String NEWS_TYPE_PROMOTION = "PROMOTION";
    public static final String NEWS_TYPE_STOCK = "STOCK";

    // ── News Post Priority constants ─────────────────────────────
    public static final String NEWS_PRIORITY_NORMAL = "NORMAL";
    public static final String NEWS_PRIORITY_IMPORTANT = "IMPORTANT";
    public static final String NEWS_PRIORITY_URGENT = "URGENT";

    // ── News Post Target Type constants ──────────────────────────
    public static final String NEWS_TARGET_ALL = "ALL";
    public static final String NEWS_TARGET_ROLE = "ROLE";
    public static final String NEWS_TARGET_SHIFT = "SHIFT";

    // ── Chat Room Type constants ─────────────────────────────────────
    public static final String CHAT_TYPE_SHIFT = "SHIFT";
    public static final String CHAT_TYPE_ROLE = "ROLE";
    public static final String CHAT_TYPE_DIRECT = "DIRECT";
    public static final String CHAT_TYPE_GROUP = "GROUP";

    // ── Chat Participant Role constants ─────────────────────────────
    public static final String CHAT_ROLE_OWNER = "OWNER";
    public static final String CHAT_ROLE_MODERATOR = "MODERATOR";
    public static final String CHAT_ROLE_MEMBER = "MEMBER";

    private Constants() {
    }
}
