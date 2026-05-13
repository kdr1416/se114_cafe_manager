package com.example.cafe_manager.util;

public final class StatusUtils {
    private StatusUtils() {
    }

    public static boolean isValidTableStatus(String status) {
        return Constants.TABLE_EMPTY.equals(status)
                || Constants.TABLE_OCCUPIED.equals(status);
    }

    public static boolean isValidOrderStatus(String status) {
        return Constants.ORDER_OPEN.equals(status)
                || Constants.ORDER_CONFIRMED.equals(status)
                || Constants.ORDER_PAID.equals(status)
                || Constants.ORDER_CANCELLED.equals(status);
    }

    public static boolean isValidPaymentMethod(String paymentMethod) {
        return Constants.PAYMENT_CASH.equals(paymentMethod)
                || Constants.PAYMENT_BANKING.equals(paymentMethod)
                || Constants.PAYMENT_MOMO.equals(paymentMethod);
    }

    public static boolean isValidPaymentStatus(String status) {
        return Constants.PAYMENT_SUCCESS.equals(status)
                || Constants.PAYMENT_FAILED.equals(status);
    }
}
