package com.example.cafe_manager.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.cafe_manager.data.local.entity.NewsReadEntity;

import java.util.List;

@Dao
public interface NewsReadDao {
    @Insert
    long insert(NewsReadEntity read);

    @Query("SELECT * FROM news_reads WHERE post_id = :postId AND user_id = :userId LIMIT 1")
    NewsReadEntity getByPostAndUser(int postId, int userId);

    @Query("SELECT COUNT(*) FROM news_reads WHERE post_id = :postId")
    int countReadByPost(int postId);

    @Query("SELECT * FROM news_reads WHERE user_id = :userId ORDER BY readAt DESC")
    LiveData<List<NewsReadEntity>> getByUser(int userId);

    // Đánh dấu đã đọc (nếu chưa có thì insert)
    @Transaction
    default void markReadIfNeeded(int postId, int userId) {
        if (getByPostAndUser(postId, userId) == null) {
            NewsReadEntity r = new NewsReadEntity();
            r.setPostId(postId);
            r.setUserId(userId);
            r.setReadAt(System.currentTimeMillis());
            insert(r);
        }
    }
}
