package com.example.cafe_manager.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.cafe_manager.data.local.entity.PaymentEntity;

@Dao
public interface PaymentDao {

    @Insert
    void insert(PaymentEntity payment);
    @Query("SELECT * FROM payments WHERE order_id = :orderId LIMIT 1")
    PaymentEntity getByOrderId(int orderId);
    @Query("SELECT * FROM payments WHERE order_id = :orderId LIMIT 1")
    LiveData<PaymentEntity> getByOrderIdLive(int orderId);
}