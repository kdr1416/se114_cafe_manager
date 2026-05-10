package com.example.cafe_manager.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.cafe_manager.data.local.entity.PaymentEntity;

@Dao
public abstract class PaymentTransactionDao {

    @Insert
    public abstract long insertPaymentInternal(PaymentEntity payment);

    @Query("UPDATE orders SET status = :status, paid_at = :paidAt WHERE order_id = :orderId")
    public abstract int updateOrderStatusWithPaidAtInternal(
            int orderId,
            String status,
            long paidAt
    );

    @Query("UPDATE tables SET status = :status WHERE table_id = :tableId")
    public abstract int updateTableStatusInternal(int tableId, String status);

    @Transaction
    public long payOrderAtomic(
            PaymentEntity payment,
            int orderId,
            int tableId,
            String paidStatus,
            String emptyTableStatus,
            long paidAt
    ) {
        long paymentId = insertPaymentInternal(payment);

        updateOrderStatusWithPaidAtInternal(orderId, paidStatus, paidAt);
        updateTableStatusInternal(tableId, emptyTableStatus);

        return paymentId;
    }
}