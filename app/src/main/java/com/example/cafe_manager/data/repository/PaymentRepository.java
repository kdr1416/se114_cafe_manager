package com.example.cafe_manager.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.dao.PaymentDao;
import com.example.cafe_manager.data.local.dao.PaymentTransactionDao;
import com.example.cafe_manager.data.local.entity.PaymentEntity;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.RepositoryCallback;

public class PaymentRepository {

    private final PaymentDao paymentDao;
    private final PaymentTransactionDao paymentTransactionDao;
    private final AppExecutors appExecutors;

    public PaymentRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.paymentDao = db.paymentDao();
        this.paymentTransactionDao = db.paymentTransactionDao();
        this.appExecutors = AppExecutors.getInstance();
    }

    /**
     * Atomic: insert Payment → update Order = PAID → update Table = EMPTY.
     */
    public void payOrder(
            int orderId,
            int tableId,
            String paymentMethod,
            double subtotal,
            double discount,
            double finalAmount,
            RepositoryCallback<Boolean> callback
    ) {
        appExecutors.diskIO().execute(() -> {
            try {
                long paidAt = System.currentTimeMillis();

                PaymentEntity payment = new PaymentEntity();
                payment.setOrderId(orderId);
                payment.setPaymentMethod(paymentMethod);
                payment.setSubtotal(subtotal);
                payment.setDiscountAmount(discount);
                payment.setFinalAmount(finalAmount);
                payment.setPaidAt(paidAt);
                payment.setStatus(Constants.PAYMENT_SUCCESS);

                paymentTransactionDao.payOrderAtomic(
                        payment,
                        orderId,
                        tableId,
                        Constants.ORDER_PAID,
                        Constants.TABLE_EMPTY,
                        paidAt
                );

                appExecutors.mainThread().execute(() ->
                        callback.onSuccess(true)
                );
            } catch (Exception e) {
                appExecutors.mainThread().execute(() ->
                        callback.onError(e)
                );
            }
        });
    }

    public LiveData<PaymentEntity> getPaymentByOrder(int orderId) {
        return paymentDao.getByOrderIdLive(orderId);
    }

}