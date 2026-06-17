package com.example.cafe_manager.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.cafe_manager.data.local.entity.OrderItemEntity;
import com.example.cafe_manager.model.TopProductRow;

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

    // ── Dashboard: top sản phẩm bán chạy trong khoảng ────────────
    @Query("SELECT product_name_snapshot AS product_name, " +
            "       SUM(quantity) AS total_quantity, " +
            "       SUM(subtotal) AS total_revenue " +
            "FROM order_items oi " +
            "INNER JOIN orders o ON oi.order_id = o.order_id " +
            "WHERE o.status = :status " +
            "  AND o.paid_at BETWEEN :fromMs AND :toMs " +
            "GROUP BY product_name_snapshot " +
            "ORDER BY total_quantity DESC " +
            "LIMIT :limit")
    LiveData<List<TopProductRow>> getTopProducts(
            String status, long fromMs, long toMs, int limit);
}
