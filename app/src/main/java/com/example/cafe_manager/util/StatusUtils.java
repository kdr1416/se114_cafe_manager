package com.example.cafe_manager.util;

public final class StatusUtils {
    private StatusUtils() {
    }

    // ========================
    // Validation
    // ========================

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

    // ========================
    // Display name (Vietnamese)
    // ========================

    /**
     * Trả về tên hiển thị tiếng Việt cho status bàn / order / payment.
     * Trả về chính status nếu không khớp (an toàn fallback).
     */
    public static String getDisplayName(String status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case Constants.TABLE_EMPTY:
                return "Trống";
            case Constants.TABLE_OCCUPIED:
                return "Có khách";

            case Constants.ORDER_OPEN:
                return "Đang mở";
            case Constants.ORDER_CONFIRMED:
                return "Đang phục vụ";
            case Constants.ORDER_PAID:
                return "Đã thanh toán";
            case Constants.ORDER_CANCELLED:
                return "Đã huỷ";

            case Constants.PAYMENT_SUCCESS:
                return "Thành công";
            case Constants.PAYMENT_FAILED:
                return "Thất bại";

            default:
                return status;
        }
    }

    /**
     * Tên hiển thị cho phương thức thanh toán.
     */
    public static String getPaymentMethodDisplayName(String paymentMethod) {
        if (paymentMethod == null) {
            return "";
        }
        switch (paymentMethod) {
            case Constants.PAYMENT_CASH:
                return "Tiền mặt";
            case Constants.PAYMENT_BANKING:
                return "Chuyển khoản";
            case Constants.PAYMENT_MOMO:
                return "Ví MoMo";
            default:
                return paymentMethod;
        }
    }
}
