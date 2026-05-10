package com.example.cafe_manager.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.cafe_manager.data.local.entity.OrderItemEntity;

import java.util.List;

@Dao
public interface OrderItemDao {

    @Insert
    void insertAll(List<OrderItemEntity> items);

    @Query("SELECT * FROM order_items WHERE order_id = :orderId")
    LiveData<List<OrderItemEntity>> getByOrderId(int orderId);

    @Query("SELECT * FROM order_items WHERE order_id = :orderId")
    List<OrderItemEntity> getByOrderIdSync(int orderId);

    @Query("DELETE FROM order_items WHERE order_id = :orderId")
    void deleteByOrderId(int orderId);
}