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

    @Query("UPDATE orders SET status = :status WHERE order_id = :orderId")
    protected abstract int updateOrderStatusInternal(int orderId, String status);

    @Query("UPDATE orders SET total_amount = total_amount + :delta WHERE order_id = :orderId")
    protected abstract int incrementOrderTotalInternal(int orderId, double delta);

    @Transaction
    public long confirmOrderAtomic(
            OrderEntity order,
            List<OrderItemEntity> items,
            int tableId,
            String occupiedStatus,
            int createdByUserId,
            int createdShiftId
    ) {
        // Set shift/user info vào order trước khi insert
        order.setCreatedByUserId(createdByUserId);
        order.setCreatedShiftId(createdShiftId);

        long orderId = insertOrderInternal(order);

        for (OrderItemEntity item : items) {
            item.setOrderId((int) orderId);
        }

        insertOrderItemsInternal(items);
        updateTableStatusInternal(tableId, occupiedStatus);

        return orderId;
    }

    /**
     * Atomic: update order = CANCELLED + table = EMPTY.
     * Dùng khi nhân viên hủy 1 order đang phục vụ.
     */
    @Transaction
    public void cancelOrderAtomic(
            int orderId,
            int tableId,
            String cancelledStatus,
            String emptyTableStatus
    ) {
        updateOrderStatusInternal(orderId, cancelledStatus);
        updateTableStatusInternal(tableId, emptyTableStatus);
    }

    /**
     * Atomic: insert thêm items vào order existing + cộng dồn total_amount.
     * Bàn vẫn OCCUPIED — không update.
     */
    @Transaction
    public void addItemsToOrderAtomic(
            int orderId,
            List<OrderItemEntity> newItems,
            double deltaAmount
    ) {
        for (OrderItemEntity item : newItems) {
            item.setOrderId(orderId);
        }

        insertOrderItemsInternal(newItems);
        incrementOrderTotalInternal(orderId, deltaAmount);
    }
}
