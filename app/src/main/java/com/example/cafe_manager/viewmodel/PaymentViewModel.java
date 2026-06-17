package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.local.entity.PromotionEntity;
import com.example.cafe_manager.data.repository.PaymentRepository;
import com.example.cafe_manager.data.repository.PromotionRepository;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.CurrencyUtils;
import com.example.cafe_manager.util.OrderCalculator;
import com.example.cafe_manager.util.RepositoryCallback;
import com.example.cafe_manager.util.StatusUtils;

public class PaymentViewModel extends AndroidViewModel {

    private final PaymentRepository paymentRepository;
    private final PromotionRepository promotionRepository;

    private int orderId = -1;
    private int tableId = -1;
    private double subtotal = 0.0;

    private final MutableLiveData<Double> subtotalLiveData = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> discountAmountLiveData = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> finalAmountLiveData = new MutableLiveData<>(0.0);

    private final MutableLiveData<String> selectedPaymentMethodLiveData =
            new MutableLiveData<>(Constants.PAYMENT_CASH);

    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> paySuccessLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> promoMessageLiveData = new MutableLiveData<>();

    public PaymentViewModel(@NonNull Application application) {
        super(application);
        this.paymentRepository = new PaymentRepository(application);
        this.promotionRepository = new PromotionRepository(application);
    }

    // ========================
    // Init từ Activity (sau khi parse Intent extras)
    // ========================

    public void setOrderInfo(int orderId, int tableId, double subtotal) {
        this.orderId = orderId;
        this.tableId = tableId;
        this.subtotal = Math.max(subtotal, 0);

        subtotalLiveData.setValue(this.subtotal);
        discountAmountLiveData.setValue(0.0);
        recomputeFinalAmount();
    }

    // ========================
    // Getters cho LiveData
    // ========================

    public LiveData<Double> getSubtotal() {
        return subtotalLiveData;
    }

    public LiveData<Double> getDiscountAmount() {
        return discountAmountLiveData;
    }

    public LiveData<Double> getFinalAmount() {
        return finalAmountLiveData;
    }

    public LiveData<String> getSelectedPaymentMethod() {
        return selectedPaymentMethodLiveData;
    }

    public LiveData<Boolean> getLoading() {
        return loadingLiveData;
    }

    public LiveData<Boolean> getPaySuccess() {
        return paySuccessLiveData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessageLiveData;
    }

    public LiveData<String> getPromoMessage() {
        return promoMessageLiveData;
    }

    public LiveData<java.util.List<com.example.cafe_manager.data.local.entity.PromotionEntity>> getAllPromotions() {
        return promotionRepository.getAll();
    }

    public void clearPaySuccess() {
        paySuccessLiveData.setValue(null);
    }

    public void clearErrorMessage() {
        errorMessageLiveData.setValue(null);
    }

    public void clearPromoMessage() {
        promoMessageLiveData.setValue(null);
    }

    // ========================
    // User actions
    // ========================

    public void setDiscount(double discount) {
        double normalized = Math.max(discount, 0);

        if (normalized > subtotal) {
            normalized = subtotal;
        }

        discountAmountLiveData.setValue(normalized);
        recomputeFinalAmount();
    }

    /**
     * Apply discount theo input từ user: có thể là số (raw VND) hoặc mã giảm giá.
     * Trống → discount = 0.
     * Số → set raw VND.
     * Chuỗi không phải số → query promotion code (async).
     */
    public void applyPromotionCode(String text) {
        if (text == null || text.trim().isEmpty()) {
            setDiscount(0);
            promoMessageLiveData.setValue("Đã xoá giảm giá.");
            return;
        }

        String input = text.trim();

        // Thử parse số trước
        try {
            double value = Double.parseDouble(input);
            setDiscount(value);
            promoMessageLiveData.setValue(
                    "Đã áp dụng giảm giá: " + CurrencyUtils.formatVnd(value));
            return;
        } catch (NumberFormatException ignored) {
            // không phải số → thử như mã promo
        }

        // Query DB trên background
        AppExecutors.getInstance().diskIO().execute(() -> {
            final PromotionEntity promo = promotionRepository.getByCode(input);
            AppExecutors.getInstance().mainThread().execute(() -> {
                if (promo == null) {
                    promoMessageLiveData.setValue("Mã giảm giá không hợp lệ.");
                    return;
                }
                if (!promo.isActive()) {
                    promoMessageLiveData.setValue("Mã giảm giá đã ngưng sử dụng.");
                    return;
                }
                if (promo.getExpiresAt() > 0
                        && promo.getExpiresAt() < System.currentTimeMillis()) {
                    promoMessageLiveData.setValue("Mã giảm giá đã hết hạn.");
                    return;
                }

                double discount;
                if (Constants.PROMO_CASH.equals(promo.getType())) {
                    discount = promo.getValue();
                } else if (Constants.PROMO_PERCENT.equals(promo.getType())) {
                    discount = subtotal * promo.getValue() / 100.0;
                } else {
                    promoMessageLiveData.setValue("Loại mã không được hỗ trợ.");
                    return;
                }

                setDiscount(discount);
                promoMessageLiveData.setValue(
                        "Áp dụng mã " + promo.getCode() + ": -"
                                + CurrencyUtils.formatVnd(discount));
            });
        });
    }

    public void selectPaymentMethod(String paymentMethod) {
        if (!StatusUtils.isValidPaymentMethod(paymentMethod)) {
            return;
        }

        selectedPaymentMethodLiveData.setValue(paymentMethod);
    }

    public void confirmPayment() {

        if (orderId == -1 || tableId == -1) {
            errorMessageLiveData.setValue("Thiếu thông tin order hoặc bàn.");
            return;
        }

        String paymentMethod = selectedPaymentMethodLiveData.getValue();

        if (!StatusUtils.isValidPaymentMethod(paymentMethod)) {
            errorMessageLiveData.setValue("Phương thức thanh toán không hợp lệ.");
            return;
        }

        double discount = discountAmountLiveData.getValue() == null
                ? 0
                : discountAmountLiveData.getValue();

        double finalAmount = OrderCalculator.calculateFinalAmount(subtotal, discount);

        loadingLiveData.setValue(true);

        paymentRepository.payOrder(
                orderId,
                tableId,
                paymentMethod,
                subtotal,
                discount,
                finalAmount,
                new RepositoryCallback<Boolean>() {

                    @Override
                    public void onSuccess(Boolean result) {

                        loadingLiveData.setValue(false);

                        paySuccessLiveData.setValue(Boolean.TRUE.equals(result));
                    }

                    @Override
                    public void onError(Exception exception) {

                        loadingLiveData.setValue(false);

                        errorMessageLiveData.setValue(
                                exception != null
                                        ? exception.getMessage()
                                        : "Thanh toán thất bại."
                        );
                    }
                }
        );
    }

    // ========================
    // Helper
    // ========================

    public int getOrderId() {
        return orderId;
    }

    public int getTableId() {
        return tableId;
    }

    private void recomputeFinalAmount() {
        double discount = discountAmountLiveData.getValue() == null
                ? 0
                : discountAmountLiveData.getValue();

        finalAmountLiveData.setValue(
                OrderCalculator.calculateFinalAmount(subtotal, discount)
        );
    }
}
