package com.example.cafe_manager.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.cafe_manager.data.local.entity.PaymentEntity;
import com.example.cafe_manager.model.DailyRevenueRow;
import com.example.cafe_manager.model.PaymentMethodStatsRow;

import java.util.List;

@Dao
public interface PaymentDao {

    @Insert
    void insert(PaymentEntity payment);

    @Query("SELECT * FROM payments WHERE order_id = :orderId LIMIT 1")
    PaymentEntity getByOrderId(int orderId);

    @Query("SELECT * FROM payments WHERE order_id = :orderId LIMIT 1")
    LiveData<PaymentEntity> getByOrderIdLive(int orderId);

    // ── Dashboard queries ──────────────────────────────────────

    @Query("SELECT COALESCE(SUM(final_amount), 0) FROM payments " +
            "WHERE paid_at BETWEEN :fromMs AND :toMs")
    LiveData<Double> getRevenueInRange(long fromMs, long toMs);

    @Query("SELECT COUNT(*) FROM payments WHERE paid_at BETWEEN :fromMs AND :toMs")
    LiveData<Integer> countPaymentsInRange(long fromMs, long toMs);

    @Query("SELECT payment_method, " +
            "COUNT(*) AS order_count, " +
            "COALESCE(SUM(final_amount), 0) AS total_revenue " +
            "FROM payments WHERE paid_at BETWEEN :fromMs AND :toMs " +
            "GROUP BY payment_method")
    LiveData<List<PaymentMethodStatsRow>> getPaymentMethodStats(long fromMs, long toMs);

    @Query("SELECT strftime('%d/%m', paid_at / 1000, 'unixepoch', 'localtime') AS day_label, " +
            "COALESCE(SUM(final_amount), 0) AS daily_revenue " +
            "FROM payments WHERE paid_at BETWEEN :fromMs AND :toMs " +
            "GROUP BY day_label ORDER BY paid_at ASC")
    LiveData<List<DailyRevenueRow>> getDailyRevenue(long fromMs, long toMs);

    /** Lấy các payment thuộc ca X. */
    @Query("SELECT * FROM payments WHERE paid_shift_id = :shiftId ORDER BY paid_at DESC")
    LiveData<List<PaymentEntity>> getByPaidShiftId(int shiftId);

    /** Tổng doanh thu tiền mặt theo ca (dùng cho reconciliation). */
    @Query("SELECT COALESCE(SUM(final_amount), 0) FROM payments " +
            "WHERE paid_shift_id = :shiftId AND payment_method = 'CASH'")
    double getCashTotalByPaidShiftId(int shiftId);

    /** Tổng doanh thu tất cả phương thức theo ca. */
    @Query("SELECT COALESCE(SUM(final_amount), 0) FROM payments " +
            "WHERE paid_shift_id = :shiftId")
    double getTotalRevenueByPaidShiftId(int shiftId);

    /** Đếm số giao dịch trong ca. */
    @Query("SELECT COUNT(*) FROM payments WHERE paid_shift_id = :shiftId")
    int countByPaidShiftId(int shiftId);

    /** Tổng doanh thu theo phương thức thanh toán trong ca (dùng cho báo cáo). */
    @Query("SELECT payment_method, " +
            "COUNT(*) AS order_count, " +
            "COALESCE(SUM(final_amount), 0) AS total_revenue " +
            "FROM payments WHERE paid_shift_id = :shiftId " +
            "GROUP BY payment_method")
    List<com.example.cafe_manager.model.PaymentMethodStatsRow> getPaymentMethodStatsByShift(int shiftId);
}
