package com.example.cafe_manager.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.cafe_manager.data.local.entity.ChatRoomEntity;

import java.util.List;

@Dao
public interface ChatRoomDao {
    @Insert
    long insert(ChatRoomEntity room);

    @Update
    void update(ChatRoomEntity room);

    @Query("SELECT * FROM chat_rooms WHERE is_active = 1 ORDER BY updated_at DESC")
    LiveData<List<ChatRoomEntity>> getAllActive();

    @Query("SELECT * FROM chat_rooms WHERE room_id = :roomId LIMIT 1")
    ChatRoomEntity getById(int roomId);

    @Query("SELECT * FROM chat_rooms WHERE shift_id = :shiftId AND is_active = 1 LIMIT 1")
    ChatRoomEntity getByShiftId(int shiftId);

    @Query("SELECT * FROM chat_rooms WHERE target_role = :role AND is_active = 1")
    LiveData<List<ChatRoomEntity>> getByRole(String role);

    @Query("SELECT cr.* FROM chat_rooms cr " +
           "INNER JOIN chat_participants cp ON cr.room_id = cp.room_id " +
           "WHERE cp.user_id = :userId AND cr.is_active = 1 AND cp.left_at IS NULL " +
           "ORDER BY cr.updated_at DESC")
    LiveData<List<ChatRoomEntity>> getByParticipant(int userId);

    @Query("SELECT cr.* FROM chat_rooms cr " +
           "INNER JOIN chat_participants cp ON cr.room_id = cp.room_id " +
           "WHERE cp.user_id = :userId AND cr.is_active = 1 AND cp.left_at IS NULL " +
           "ORDER BY cr.updated_at DESC")
    List<ChatRoomEntity> getByParticipantSync(int userId);
}
