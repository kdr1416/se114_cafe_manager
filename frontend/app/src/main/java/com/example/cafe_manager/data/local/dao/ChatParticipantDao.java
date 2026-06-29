package com.example.cafe_manager.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.cafe_manager.data.local.entity.ChatParticipantEntity;

import java.util.List;

@Dao
public interface ChatParticipantDao {
    @Insert
    long insert(ChatParticipantEntity participant);

    @Update
    void update(ChatParticipantEntity participant);

    @Query("SELECT * FROM chat_participants WHERE room_id = :roomId AND user_id = :userId LIMIT 1")
    ChatParticipantEntity getByRoomAndUser(int roomId, int userId);

    @Query("SELECT * FROM chat_participants WHERE room_id = :roomId AND left_at IS NULL")
    List<ChatParticipantEntity> getByRoom(int roomId);

    @Query("SELECT * FROM chat_participants WHERE user_id = :userId AND left_at IS NULL")
    LiveData<List<ChatParticipantEntity>> getActiveByUser(int userId);

    @Query("UPDATE chat_participants SET left_at = :leftAt WHERE room_id = :roomId AND user_id = :userId")
    void leaveRoom(int roomId, int userId, long leftAt);

    @Query("DELETE FROM chat_participants WHERE room_id = :roomId AND user_id = :userId")
    void deleteParticipant(int roomId, int userId);
}
