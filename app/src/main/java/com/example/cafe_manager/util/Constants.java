package com.example.cafe_manager.util;

public final class Constants {
    public static final String TABLE_EMPTY = "EMPTY";
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
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_MANAGER = "MANAGER";
    public static final String ROLE_STAFF = "STAFF";

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

    // ── Cash session status ─────────────────────────────────────
    public static final String CASH_SESSION_OPEN = "OPEN";
    public static final String CASH_SESSION_CLOSED = "CLOSED";
    
    private Constants() {
    }
}
