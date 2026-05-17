package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.repository.PaymentRepository;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.OrderCalculator;
import com.example.cafe_manager.util.RepositoryCallback;
import com.example.cafe_manager.util.StatusUtils;

public class PaymentViewModel extends AndroidViewModel {

    private final PaymentRepository paymentRepository;

    // Order info
    private int orderId = -1;
    private int tableId = -1;
    private double subtotal = 0.0;

    // LiveData
    private final MutableLiveData<Double> subtotalLiveData =
            new MutableLiveData<>(0.0);

    private final MutableLiveData<Double> discountAmountLiveData =
            new MutableLiveData<>(0.0);

    private final MutableLiveData<Double> finalAmountLiveData =
            new MutableLiveData<>(0.0);

    private final MutableLiveData<String> selectedPaymentMethodLiveData =
            new MutableLiveData<>(Constants.PAYMENT_CASH);

    // State
    private final MutableLiveData<Boolean> loadingLiveData =
            new MutableLiveData<>(false);

    private final MutableLiveData<Boolean> paySuccessLiveData =
            new MutableLiveData<>();

    private final MutableLiveData<String> errorMessageLiveData =
            new MutableLiveData<>();

    public PaymentViewModel(@NonNull Application application) {
        super(application);

        this.paymentRepository = new PaymentRepository(application);
    }

    // ========================
    // Setup order info
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
    // Getters
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

    // ========================
    // Clear events
    // ========================

    public void clearPaySuccess() {
        paySuccessLiveData.setValue(null);
    }

    public void clearErrorMessage() {
        errorMessageLiveData.setValue(null);
    }

    // ========================
    // User actions
    // ========================

    /**
     * Set discount
     */
    public void setDiscount(double discount) {

        double normalized = Math.max(discount, 0);

        if (normalized > subtotal) {
            normalized = subtotal;
        }

        discountAmountLiveData.setValue(normalized);

        recomputeFinalAmount();
    }

    /**
     * Select payment method
     */
    public void selectPaymentMethod(String paymentMethod) {

        if (!StatusUtils.isValidPaymentMethod(paymentMethod)) {
            return;
        }

        selectedPaymentMethodLiveData.setValue(paymentMethod);
    }

    /**
     * Confirm payment
     */
    public void confirmPayment() {

        // Validate order
        if (orderId == -1 || tableId == -1) {

            errorMessageLiveData.setValue(
                    "Thiếu thông tin order hoặc bàn."
            );

            return;
        }

        // Validate payment method
        String paymentMethod =
                selectedPaymentMethodLiveData.getValue();

        if (!StatusUtils.isValidPaymentMethod(paymentMethod)) {

            errorMessageLiveData.setValue(
                    "Phương thức thanh toán không hợp lệ."
            );

            return;
        }

        // Get discount
        double discount =
                discountAmountLiveData.getValue() == null
                        ? 0
                        : discountAmountLiveData.getValue();

        // Calculate final amount
        double finalAmount =
                OrderCalculator.calculateFinalAmount(
                        subtotal,
                        discount
                );

        // Start loading
        loadingLiveData.setValue(true);

        // Call repository
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

                        paySuccessLiveData.setValue(
                                Boolean.TRUE.equals(result)
                        );
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
    // Helpers
    // ========================

    public int getOrderId() {
        return orderId;
    }

    public int getTableId() {
        return tableId;
    }

    /**
     * Recompute final amount
     */
    private void recomputeFinalAmount() {

        double discount =
                discountAmountLiveData.getValue() == null
                        ? 0
                        : discountAmountLiveData.getValue();

        finalAmountLiveData.setValue(
                OrderCalculator.calculateFinalAmount(
                        subtotal,
                        discount
                )
        );
    }
}
