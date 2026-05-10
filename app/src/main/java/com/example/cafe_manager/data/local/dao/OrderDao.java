package com.example.cafe_manager.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.cafe_manager.data.local.entity.OrderEntity;

@Dao
public interface OrderDao {

    @Insert
    long insert(OrderEntity order);
    @Query("SELECT * FROM orders WHERE table_id = :tableId AND status = :status ORDER BY order_id DESC LIMIT 1")
    LiveData<OrderEntity> getActiveByTableIdLive(int tableId, String status);

    @Query("SELECT * FROM orders WHERE table_id = :tableId AND status = :status ORDER BY order_id DESC LIMIT 1")
    OrderEntity getActiveByTableId(int tableId, String status);

    @Query("SELECT * FROM orders WHERE order_id = :orderId")
    OrderEntity getById(int orderId);

    @Query("SELECT * FROM orders WHERE order_id = :orderId")
    LiveData<OrderEntity> getByIdLive(int orderId);

    @Query("UPDATE orders SET status = :status WHERE order_id = :orderId")
    void updateStatus(int orderId, String status);

    @Query("UPDATE orders SET status = :status, paid_at = :paidAt WHERE order_id = :orderId")
    void updateStatusWithPaidAt(int orderId, String status, long paidAt);

    @Query("UPDATE orders SET total_amount = :totalAmount WHERE order_id = :orderId")
    void updateTotal(int orderId, double totalAmount);

}
