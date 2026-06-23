package com.example.cafe_manager.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.dao.NewsPostDao;
import com.example.cafe_manager.data.local.dao.NewsReadDao;
import com.example.cafe_manager.data.local.entity.NewsPostEntity;
import com.example.cafe_manager.util.AppExecutors;

import java.util.List;

public class NewsRepository {
    private final NewsPostDao postDao;
    private final NewsReadDao readDao;
    private final AppExecutors exec;

    public NewsRepository(Context ctx) {
        AppDatabase db = AppDatabase.getInstance(ctx);
        this.postDao = db.newsPostDao();
        this.readDao = db.newsReadDao();
        this.exec = AppExecutors.getInstance();
    }

    // LiveData for UI
    public LiveData<List<NewsPostEntity>> getVisiblePosts() {
        return postDao.getAllVisible();
    }

    public LiveData<List<NewsPostEntity>> getUnreadPosts(int userId) {
        return postDao.getUnreadForUser(userId);
    }

    public LiveData<Integer> getUnreadCount(int userId) {
        return postDao.getUnreadCountForUser(userId);
    }

    public LiveData<List<NewsPostEntity>> getPinnedPosts() {
        return postDao.getPinned();
    }

    public LiveData<List<NewsPostEntity>> getUrgentPosts() {
        return postDao.getUrgent();
    }

    // CRUD (admin/manager)
    public void createPost(NewsPostEntity post, Runnable onSuccess, Runnable onError) {
        exec.diskIO().execute(() -> {
            try {
                postDao.insert(post);
                exec.mainThread().execute(onSuccess);
            } catch (Exception e) {
                exec.mainThread().execute(onError);
            }
        });
    }

    public void updatePost(NewsPostEntity post, Runnable onSuccess) {
        exec.diskIO().execute(() -> {
            postDao.update(post);
            exec.mainThread().execute(onSuccess);
        });
    }

    public void deletePost(int postId) {
        exec.diskIO().execute(() -> postDao.softDelete(postId));
    }

    // Read tracking
    public void markPostRead(int postId, int userId) {
        exec.diskIO().execute(() -> readDao.markReadIfNeeded(postId, userId));
    }

    public NewsPostEntity getPostById(int postId) {
        return postDao.getById(postId);
    }
}
