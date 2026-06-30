package com.example.cafe_manager.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.cafe_manager.data.local.entity.ShiftCashSessionEntity;

@Dao
public interface ShiftCashSessionDao {

    @Insert
    long insert(ShiftCashSessionEntity session);

    @Query("SELECT * FROM shift_cash_sessions WHERE shift_id = :shiftId LIMIT 1")
    ShiftCashSessionEntity getByShift(int shiftId);

    @Query("SELECT * FROM shift_cash_sessions WHERE shift_id = :shiftId LIMIT 1")
    LiveData<ShiftCashSessionEntity> getByShiftLive(int shiftId);

    @Query("SELECT * FROM shift_cash_sessions WHERE status = 'OPEN' LIMIT 1")
    ShiftCashSessionEntity getOpenSession();

    @Query("UPDATE shift_cash_sessions SET " +
            "closing_cash = :closingCash, " +
            "actual_cash = :actualCash, " +
            "expected_cash = :expectedCash, " +
            "cash_difference = :cashDifference, " +
            "closed_by = :closedBy, " +
            "closed_at = :closedAt, " +
            "status = 'CLOSED' " +
            "WHERE session_id = :sessionId")
    void closeSession(int sessionId, double closingCash, double actualCash,
                      double expectedCash, double cashDifference,
                      int closedBy, long closedAt);
}
