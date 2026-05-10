package com.example.cafe_manager.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.cafe_manager.data.local.entity.OrderEntity;
import com.example.cafe_manager.data.local.entity.OrderItemEntity;

import java.util.List;

@Dao
public abstract class OrderTransactionDao {

    @Insert
    protected abstract long insertOrderInternal(OrderEntity order);

    @Insert
    protected abstract void insertOrderItemsInternal(List<OrderItemEntity> items);

    @Query("UPDATE tables SET status = :status WHERE table_id = :tableId")
    protected abstract int updateTableStatusInternal(int tableId, String status);

    @Transaction
    public long confirmOrderAtomic(
            OrderEntity order,
            List<OrderItemEntity> items,
            int tableId,
            String occupiedStatus
    ) {
        long orderId = insertOrderInternal(order);

        for (OrderItemEntity item : items) {
            item.setOrderId((int) orderId);
        }

        insertOrderItemsInternal(items);
        updateTableStatusInternal(tableId, occupiedStatus);

        return orderId;
    }
}