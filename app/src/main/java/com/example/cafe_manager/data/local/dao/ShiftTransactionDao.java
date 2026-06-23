package com.example.cafe_manager.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.cafe_manager.data.local.entity.ShiftCashSessionEntity;
import com.example.cafe_manager.data.local.entity.ShiftEntity;
import com.example.cafe_manager.util.Constants;

@Dao
public abstract class ShiftTransactionDao {

    @Query("SELECT * FROM shifts WHERE status = 'IN_PROGRESS' LIMIT 1")
    protected abstract ShiftEntity getCurrentlyOpenShiftInternal();

    @Query("SELECT * FROM shift_cash_sessions WHERE status = 'OPEN' LIMIT 1")
    protected abstract ShiftCashSessionEntity getOpenCashSessionInternal();

    @Query("SELECT * FROM shifts WHERE shift_id = :shiftId LIMIT 1")
    protected abstract ShiftEntity getShiftByIdInternal(int shiftId);

    @Query("SELECT * FROM shift_cash_sessions WHERE shift_id = :shiftId LIMIT 1")
    protected abstract ShiftCashSessionEntity getCashSessionByShiftInternal(int shiftId);

    @Query("UPDATE shifts SET status = 'IN_PROGRESS', opened_by = :openedBy, opened_at = :openedAt WHERE shift_id = :shiftId")
    protected abstract int openShiftInternal(int shiftId, int openedBy, long openedAt);

    @Query("UPDATE shifts SET status = 'CLOSED', closed_by = :closedBy, closed_at = :closedAt WHERE shift_id = :shiftId")
    protected abstract int closeShiftInternal(int shiftId, int closedBy, long closedAt);

    @Insert
    protected abstract long insertCashSessionInternal(ShiftCashSessionEntity session);

    @Query("SELECT COALESCE(SUM(final_amount), 0) FROM payments WHERE paid_shift_id = :shiftId AND payment_method = :paymentMethod")
    protected abstract double getCashTotalByPaidShiftIdInternal(int shiftId, String paymentMethod);

    @Query("UPDATE shift_cash_sessions SET " +
            "closing_cash = :closingCash, " +
            "actual_cash = :actualCash, " +
            "expected_cash = :expectedCash, " +
            "cash_difference = :cashDifference, " +
            "closed_by = :closedBy, " +
            "closed_at = :closedAt, " +
            "status = 'CLOSED' " +
            "WHERE session_id = :sessionId")
    protected abstract int closeCashSessionInternal(int sessionId, double closingCash, double actualCash,
                                                    double expectedCash, double cashDifference,
                                                    int closedBy, long closedAt);

    @Transaction
    public void openShiftWithCashAtomic(int shiftId, double openingCash, int userId, long openedAt) {
        if (openingCash < 0) {
            throw new RuntimeException("Tiền két mở ca không được âm.");
        }

        ShiftEntity shift = getShiftByIdInternal(shiftId);
        if (shift == null) {
            throw new RuntimeException("Ca làm việc không tồn tại.");
        }
        if (!Constants.SHIFT_PUBLISHED.equals(shift.getStatus())) {
            throw new RuntimeException("Chỉ có thể mở ca làm việc ở trạng thái PUBLISHED.");
        }

        ShiftEntity openShift = getCurrentlyOpenShiftInternal();
        if (openShift != null) {
            throw new RuntimeException("Có ca làm việc khác đang hoạt động.");
        }

        ShiftCashSessionEntity openSession = getOpenCashSessionInternal();
        if (openSession != null) {
            throw new RuntimeException("Có phiên két tiền mặt khác đang mở.");
        }

        // Cập nhật ca thành IN_PROGRESS
        openShiftInternal(shiftId, userId, openedAt);

        // Chèn phiên két tiền mặt OPEN
        ShiftCashSessionEntity session = new ShiftCashSessionEntity();
        session.setShiftId(shiftId);
        session.setOpeningCash(openingCash);
        session.setOpenedBy(userId);
        session.setOpenedAt(openedAt);
        session.setStatus(Constants.CASH_SESSION_OPEN);
        insertCashSessionInternal(session);
    }

    @Transaction
    public void closeShiftWithCashAtomic(int shiftId, double actualCash, int closedBy, long closedAt, String cashMethod) {
        if (actualCash < 0) {
            throw new RuntimeException("Số tiền thực tế không được âm.");
        }

        ShiftCashSessionEntity session = getCashSessionByShiftInternal(shiftId);
        if (session == null) {
            throw new RuntimeException("Không tìm thấy phiên tiền mặt cho ca này.");
        }
        if (!Constants.CASH_SESSION_OPEN.equals(session.getStatus())) {
            throw new RuntimeException("Phiên tiền mặt này đã được đóng hoặc không ở trạng thái mở.");
        }

        ShiftEntity shift = getShiftByIdInternal(shiftId);
        if (shift == null) {
            throw new RuntimeException("Ca làm việc không tồn tại.");
        }
        if (!Constants.SHIFT_IN_PROGRESS.equals(shift.getStatus())) {
            throw new RuntimeException("Ca làm việc không ở trạng thái đang chạy (IN_PROGRESS).");
        }

        // Tính expectedCash = openingCash + tổng CASH payments
        double cashPaymentsTotal = getCashTotalByPaidShiftIdInternal(shiftId, cashMethod);
        double expectedCash = session.getOpeningCash() + cashPaymentsTotal;
        double difference = actualCash - expectedCash;

        closeCashSessionInternal(
                session.getSessionId(),
                actualCash, // closingCash
                actualCash, // actualCash
                expectedCash,
                difference,
                closedBy,
                closedAt
        );

        closeShiftInternal(shiftId, closedBy, closedAt);
    }
}
