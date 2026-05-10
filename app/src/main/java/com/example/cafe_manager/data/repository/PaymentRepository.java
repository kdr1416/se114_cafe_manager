package com.example.cafe_manager.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.dao.OrderDao;
import com.example.cafe_manager.data.local.dao.PaymentDao;
import com.example.cafe_manager.data.local.dao.TableDao;
import com.example.cafe_manager.data.local.entity.PaymentEntity;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.RepositoryCallback;

public class PaymentRepository {

    private final AppDatabase db;
    private final PaymentDao paymentDao;
    private final OrderDao orderDao;
    private final TableDao tableDao;
    private final AppExecutors appExecutors;

    public PaymentRepository(Context context) {
        this.db = AppDatabase.getInstance(context);
        this.paymentDao = db.paymentDao();
        this.orderDao = db.orderDao();
        this.tableDao = db.tableDao();
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
                db.runInTransaction(() -> {
                    long paidAt = System.currentTimeMillis();

                    // 1. Insert PaymentEntity
                    PaymentEntity payment = new PaymentEntity();
                    payment.setOrderId(orderId);
                    payment.setPaymentMethod(paymentMethod);
                    payment.setSubtotal(subtotal);
                    payment.setDiscountAmount(discount);
                    payment.setFinalAmount(finalAmount);
                    payment.setPaidAt(paidAt);
                    payment.setStatus(Constants.PAYMENT_SUCCESS);
                    paymentDao.insert(payment);

                    // 2. Update order → PAID + paidAt
                    orderDao.updateStatusWithPaidAt(orderId, Constants.ORDER_PAID, paidAt);

                    // 3. Update bàn → EMPTY
                    tableDao.updateStatus(tableId, Constants.TABLE_EMPTY);
                });

                callback.onSuccess(true);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public LiveData<PaymentEntity> getPaymentByOrder(int orderId) {
        return paymentDao.getByOrderIdLive(orderId);
    }

}