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

    /** Sync: lấy danh sách phân công theo ca (cho dialog). */
    @Query("SELECT * FROM shift_assignments WHERE shift_id = :shiftId")
    List<ShiftAssignmentEntity> getByShiftSync(int shiftId);

    /** Sync: lấy danh sách phân công theo user (cho MyShift). */
    @Query("SELECT * FROM shift_assignments WHERE user_id = :userId")
    List<ShiftAssignmentEntity> getByUserSync(int userId);

    /** Đếm số nhân viên đã phân công cho 1 ca. */
    @Query("SELECT COUNT(*) FROM shift_assignments WHERE shift_id = :shiftId")
    int countByShift(int shiftId);

    @Query("SELECT s.status FROM shifts s INNER JOIN shift_assignments sa ON s.shift_id = sa.shift_id WHERE sa.assignment_id = :assignmentId")
    String getShiftStatusByAssignmentId(int assignmentId);

    @Query("SELECT * FROM shift_assignments WHERE shift_id = :shiftId AND user_id = :userId LIMIT 1")
    ShiftAssignmentEntity getByShiftAndUser(int shiftId, int userId);

    @Query("SELECT COUNT(*) FROM shift_assignments sa " +
           "INNER JOIN shifts s ON sa.shift_id = s.shift_id " +
           "WHERE sa.user_id = :userId " +
           "AND s.shift_date BETWEEN :fromDate AND :toDate " +
           "AND s.status != 'CANCELLED'")
    int countAssignmentsInRange(int userId, long fromDate, long toDate);

    @Query("SELECT sa.* FROM shift_assignments sa " +
           "INNER JOIN shifts s ON sa.shift_id = s.shift_id " +
           "WHERE sa.user_id = :userId " +
           "AND s.shift_date BETWEEN :fromDate AND :toDate " +
           "AND s.status != 'CANCELLED'")
    List<ShiftAssignmentEntity> getAssignmentsInRange(int userId, long fromDate, long toDate);

    @Query("SELECT shift_id FROM shift_assignments WHERE assignment_id = :id")
    int getShiftIdByAssignmentId(int id);

    @Query("SELECT * FROM shift_assignments WHERE assignment_id = :id LIMIT 1")
    ShiftAssignmentEntity getById(int id);
}
