package com.example.cafe_manager.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.cafe_manager.data.local.entity.ChatMessageEntity;

import java.util.List;

@Dao
public interface ChatMessageDao {
    @Insert
    long insert(ChatMessageEntity message);

    @Query("SELECT * FROM chat_messages WHERE room_id = :roomId AND is_deleted = 0 ORDER BY created_at ASC LIMIT :limit")
    LiveData<List<ChatMessageEntity>> getByRoom(int roomId, int limit);

    @Query("SELECT * FROM chat_messages WHERE room_id = :roomId AND is_deleted = 0 ORDER BY created_at DESC LIMIT 1")
    LiveData<ChatMessageEntity> getLatest(int roomId);

    @Query("SELECT COUNT(*) FROM chat_messages cm " +
           "LEFT JOIN chat_reads cr ON cm.message_id = cr.message_id AND cr.user_id = :userId " +
           "WHERE cm.room_id = :roomId AND cm.is_deleted = 0 AND cr.read_id IS NULL AND cm.sender_id != :userId")
    int getUnreadCountSync(int roomId, int userId);

    @Query("SELECT COUNT(*) FROM chat_messages cm " +
           "LEFT JOIN chat_reads cr ON cm.message_id = cr.message_id AND cr.user_id = :userId " +
           "WHERE cm.room_id = :roomId AND cm.is_deleted = 0 AND cr.read_id IS NULL AND cm.sender_id != :userId")
    LiveData<Integer> getUnreadCountLive(int roomId, int userId);

    @Query("SELECT COUNT(*) FROM chat_messages WHERE room_id = :roomId AND is_deleted = 0")
    int countByRoom(int roomId);
}
