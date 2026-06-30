package com.example.cafe_manager.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.cafe_manager.data.local.entity.NewsPostEntity;

import java.util.List;

@Dao
public interface NewsPostDao {
    @Insert
    long insert(NewsPostEntity post);

    @Update
    void update(NewsPostEntity post);

    @Query("UPDATE news_posts SET is_deleted = 1 WHERE post_id = :postId")
    void softDelete(int postId);

    @Query("SELECT * FROM news_posts WHERE is_deleted = 0 ORDER BY is_pinned DESC, createdAt DESC")
    LiveData<List<NewsPostEntity>> getAllVisible();

    @Query("SELECT * FROM news_posts WHERE is_deleted = 0 AND target_type = 'ALL' ORDER BY is_pinned DESC, createdAt DESC")
    LiveData<List<NewsPostEntity>> getForAll();

    @Query("SELECT * FROM news_posts WHERE is_deleted = 0 AND target_type = 'ROLE' AND target_role = :role ORDER BY is_pinned DESC, createdAt DESC")
    LiveData<List<NewsPostEntity>> getForRole(String role);

    @Query("SELECT * FROM news_posts WHERE is_deleted = 0 AND target_type = 'SHIFT' AND target_shift_id = :shiftId ORDER BY is_pinned DESC, createdAt DESC")
    LiveData<List<NewsPostEntity>> getForShift(int shiftId);

    @Query("SELECT * FROM news_posts WHERE post_id = :postId LIMIT 1")
    NewsPostEntity getById(int postId);

    @Query("SELECT COUNT(*) FROM news_posts WHERE is_deleted = 0 AND is_pinned = 1")
    LiveData<Integer> getPinnedCount();

    @Query("SELECT * FROM news_posts WHERE is_deleted = 0 AND is_pinned = 1 ORDER BY createdAt DESC")
    LiveData<List<NewsPostEntity>> getPinned();

    @Query("SELECT * FROM news_posts WHERE is_deleted = 0 AND priority = 'URGENT' ORDER BY is_pinned DESC, createdAt DESC")
    LiveData<List<NewsPostEntity>> getUrgent();

    // Helper để tính unread (JOIN với news_reads)
    @Query("SELECT np.* FROM news_posts np " +
           "LEFT JOIN news_reads nr ON np.post_id = nr.post_id AND nr.user_id = :userId " +
           "WHERE np.is_deleted = 0 AND nr.read_id IS NULL " +
           "ORDER BY np.is_pinned DESC, np.createdAt DESC")
    LiveData<List<NewsPostEntity>> getUnreadForUser(int userId);

    @Query("SELECT COUNT(*) FROM news_posts np " +
           "LEFT JOIN news_reads nr ON np.post_id = nr.post_id AND nr.user_id = :userId " +
           "WHERE np.is_deleted = 0 AND nr.read_id IS NULL")
    LiveData<Integer> getUnreadCountForUser(int userId);
}
