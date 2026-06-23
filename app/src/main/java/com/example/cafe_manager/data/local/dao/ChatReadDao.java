package com.example.cafe_manager.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.cafe_manager.data.local.entity.ChatReadEntity;

@Dao
public interface ChatReadDao {
    @Insert
    void insert(ChatReadEntity read);

    @Query("SELECT * FROM chat_reads WHERE message_id = :messageId AND user_id = :userId LIMIT 1")
    ChatReadEntity getByMessageAndUser(int messageId, int userId);

    @Query("SELECT COUNT(*) FROM chat_reads WHERE user_id = :userId AND message_id IN " +
           "(SELECT message_id FROM chat_messages WHERE room_id = :roomId)")
    int countReadInRoom(int userId, int roomId);

    @Query("INSERT OR IGNORE INTO chat_reads (message_id, user_id, read_at) " +
           "VALUES (:messageId, :userId, :readAt)")
    void markMessageReadIfNeeded(int messageId, int userId, long readAt);

    @Query("INSERT OR IGNORE INTO chat_reads (message_id, user_id, read_at) " +
           "SELECT message_id, :userId, :readAt FROM chat_messages " +
           "WHERE room_id = :roomId AND sender_id != :userId AND is_deleted = 0")
    void markAllMessagesRead(int roomId, int userId, long readAt);
}
