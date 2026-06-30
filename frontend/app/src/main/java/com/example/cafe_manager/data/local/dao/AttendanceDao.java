package com.example.cafe_manager.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.cafe_manager.data.local.entity.AttendanceEntity;

import java.util.List;

@Dao
public interface AttendanceDao {

    @Insert
    long insert(AttendanceEntity attendance);

    @Update
    void update(AttendanceEntity attendance);

    @Query("SELECT * FROM attendance WHERE shift_id = :shiftId")
    LiveData<List<AttendanceEntity>> getByShift(int shiftId);

    @Query("SELECT * FROM attendance WHERE shift_id = :shiftId")
    List<AttendanceEntity> getByShiftSync(int shiftId);

    @Query("SELECT * FROM attendance WHERE user_id = :userId")
    LiveData<List<AttendanceEntity>> getByUser(int userId);

    @Query("SELECT * FROM attendance WHERE shift_id = :shiftId AND user_id = :userId LIMIT 1")
    AttendanceEntity getByShiftAndUser(int shiftId, int userId);

    @Query("UPDATE attendance SET check_in_at = :checkInAt, status = :status WHERE shift_id = :shiftId AND user_id = :userId")
    void checkIn(int shiftId, int userId, long checkInAt, String status);

    @Query("UPDATE attendance SET check_out_at = :checkOutAt, late_minutes = :late, early_leave_minutes = :earlyLeave, status = :status " +
           "WHERE shift_id = :shiftId AND user_id = :userId")
    void checkOut(int shiftId, int userId, long checkOutAt, int late, int earlyLeave, String status);
}
