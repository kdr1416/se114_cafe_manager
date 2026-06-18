package com.example.cafe_manager.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.cafe_manager.data.local.entity.ShiftAssignmentEntity;

import java.util.List;

@Dao
public interface ShiftAssignmentDao {

    @Insert
    long insert(ShiftAssignmentEntity assignment);

    @Update
    void update(ShiftAssignmentEntity assignment);

    @Query("DELETE FROM shift_assignments WHERE assignment_id = :id")
    void delete(int id);

    @Query("SELECT * FROM shift_assignments WHERE shift_id = :shiftId")
    LiveData<List<ShiftAssignmentEntity>> getByShift(int shiftId);

    @Query("SELECT * FROM shift_assignments WHERE user_id = :userId")
    LiveData<List<ShiftAssignmentEntity>> getByUser(int userId);

    @Query("UPDATE shift_assignments SET confirmed = 1 WHERE assignment_id = :id")
    void confirm(int id);

    // Kiểm tra trùng ca cho nhân viên
    @Query("SELECT sa.* FROM shift_assignments sa " +
           "INNER JOIN shifts s ON sa.shift_id = s.shift_id " +
           "WHERE sa.user_id = :userId " +
           "AND s.shift_date = :date " +
           "AND s.status != 'CANCELLED' " +
           "AND ((s.start_time < :newEnd AND s.end_time > :newStart))")
    List<ShiftAssignmentEntity> getOverlapping(int userId, long date, String newStart, String newEnd);
}
