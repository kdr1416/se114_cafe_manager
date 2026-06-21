package com.example.cafe_manager.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.cafe_manager.data.local.entity.ShiftEntity;

import java.util.List;

@Dao
public interface ShiftDao {

    @Insert
    long insert(ShiftEntity shift);

    @Update
    void update(ShiftEntity shift);

    @Query("SELECT * FROM shifts ORDER BY shift_date DESC, start_time ASC")
    LiveData<List<ShiftEntity>> getAll();

    @Query("SELECT * FROM shifts WHERE shift_date = :date ORDER BY start_time ASC")
    LiveData<List<ShiftEntity>> getByDate(long date);

    @Query("SELECT * FROM shifts WHERE shift_date = :date ORDER BY start_time ASC")
    List<ShiftEntity> getByDateSync(long date);

    @Query("SELECT * FROM shifts WHERE shift_date BETWEEN :from AND :to ORDER BY shift_date ASC, start_time ASC")
    LiveData<List<ShiftEntity>> getByDateRange(long from, long to);

    @Query("SELECT * FROM shifts WHERE shift_id = :id LIMIT 1")
    ShiftEntity getById(int id);

    @Query("SELECT * FROM shifts WHERE status = 'IN_PROGRESS' LIMIT 1")
    ShiftEntity getCurrentlyOpen();

    @Query("UPDATE shifts SET status = :status WHERE shift_id = :id")
    void updateStatus(int id, String status);

    @Query("UPDATE shifts SET status = 'IN_PROGRESS', opened_by = :openedBy, opened_at = :openedAt WHERE shift_id = :id")
    void openShift(int id, int openedBy, long openedAt);

    @Query("UPDATE shifts SET status = 'CLOSED', closed_by = :closedBy, closed_at = :closedAt WHERE shift_id = :id")
    void closeShift(int id, int closedBy, long closedAt);

    /** Lấy các ca mà userId được phân công. */
    @Query("SELECT DISTINCT s.* FROM shifts s " +
           "INNER JOIN shift_assignments sa ON s.shift_id = sa.shift_id " +
           "WHERE sa.user_id = :userId " +
           "ORDER BY s.shift_date DESC, s.start_time ASC")
    LiveData<List<ShiftEntity>> getShiftsByUserId(int userId);
}
