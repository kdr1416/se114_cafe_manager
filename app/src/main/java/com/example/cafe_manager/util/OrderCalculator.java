package com.example.cafe_manager.util;

public final class OrderCalculator {
    private OrderCalculator() {
    }

    public static double calculateItemSubtotal(int quantity, double unitPrice) {
        if (quantity <= 0) {
            return 0;
        }
        return quantity * Math.max(unitPrice, 0);
    }

    public static double calculateFinalAmount(double amount, double discountAmount) {
        return Math.max(amount - Math.max(discountAmount, 0), 0);
    }
}
