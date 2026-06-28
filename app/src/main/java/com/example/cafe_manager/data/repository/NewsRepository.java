package com.example.cafe_manager.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.local.entity.NewsPostEntity;
import com.example.cafe_manager.data.remote.ApiClient;
import com.example.cafe_manager.data.remote.NewsApiService;
import com.example.cafe_manager.data.remote.NewsPostResponse;
import com.example.cafe_manager.data.remote.CreateNewsRequest;
import com.example.cafe_manager.data.remote.UpdateNewsRequest;
import com.example.cafe_manager.manager.SessionManager;
import com.example.cafe_manager.util.AppExecutors;

import java.util.ArrayList;
import java.util.List;

public class NewsRepository {
    private static volatile NewsRepository instance;
    private final NewsApiService apiService;
    private final AppExecutors exec;
    private final int currentUserId;

    // Cache for all posts from server
    private final MutableLiveData<List<NewsPostEntity>> allPostsCache = new MutableLiveData<>();
    private final MutableLiveData<Integer> unreadCountCache = new MutableLiveData<>();

    private NewsRepository(Context ctx) {
        this.apiService = ApiClient.getInstance(ctx).getService(NewsApiService.class);
        this.exec = AppExecutors.getInstance();
        this.currentUserId = SessionManager.getInstance(ctx).getUserId();
        // Initial load
        refreshPosts();
    }

    public static NewsRepository getInstance(Context ctx) {
        if (instance == null) {
            synchronized (NewsRepository.class) {
                if (instance == null) {
                    instance = new NewsRepository(ctx.getApplicationContext());
                }
            }
        }
        return instance;
    }

    // Primary data source - all posts
    public LiveData<List<NewsPostEntity>> getAllPosts() {
        return allPostsCache;
    }

    // Computed LiveData sources - filter from cache
    public LiveData<List<NewsPostEntity>> getVisiblePosts() {
        MediatorLiveData<List<NewsPostEntity>> result = new MediatorLiveData<>();
        result.addSource(allPostsCache, posts -> {
            result.setValue(posts != null ? posts : new ArrayList<>());
        });
        return result;
    }

    public LiveData<List<NewsPostEntity>> getUnreadPosts(int userId) {
        MediatorLiveData<List<NewsPostEntity>> result = new MediatorLiveData<>();
        result.addSource(allPostsCache, posts -> {
            if (posts == null) {
                result.setValue(new ArrayList<>());
                return;
            }
            List<NewsPostEntity> unread = new ArrayList<>();
            for (NewsPostEntity p : posts) {
                if (!p.getIsRead()) {
                    unread.add(p);
                }
            }
            result.setValue(unread);
        });
        return result;
    }

    public LiveData<List<NewsPostEntity>> getPinnedPosts() {
        MediatorLiveData<List<NewsPostEntity>> result = new MediatorLiveData<>();
        result.addSource(allPostsCache, posts -> {
            if (posts == null) {
                result.setValue(new ArrayList<>());
                return;
            }
            List<NewsPostEntity> pinned = new ArrayList<>();
            for (NewsPostEntity p : posts) {
                if (p.getIsPinned()) {
                    pinned.add(p);
                }
            }
            result.setValue(pinned);
        });
        return result;
    }

    public LiveData<List<NewsPostEntity>> getUrgentPosts() {
        MediatorLiveData<List<NewsPostEntity>> result = new MediatorLiveData<>();
        result.addSource(allPostsCache, posts -> {
            if (posts == null) {
                result.setValue(new ArrayList<>());
                return;
            }
            List<NewsPostEntity> urgent = new ArrayList<>();
            for (NewsPostEntity p : posts) {
                if ("URGENT".equals(p.getPriority())) {
                    urgent.add(p);
                }
            }
            result.setValue(urgent);
        });
        return result;
    }

    public LiveData<Integer> getUnreadCount(int userId) {
        MediatorLiveData<Integer> result = new MediatorLiveData<>();
        result.addSource(allPostsCache, posts -> {
            if (posts == null) {
                result.setValue(0);
                return;
            }
            int count = 0;
            for (NewsPostEntity p : posts) {
                if (!p.getIsRead()) count++;
            }
            result.setValue(count);
        });
        return result;
    }

    // CRUD operations with callbacks
    public void createPost(NewsPostEntity post, Runnable onSuccess, Runnable onError) {
        CreateNewsRequest request = new CreateNewsRequest();
        request.setTitle(post.getTitle());
        request.setContent(post.getContent());
        request.setType(post.getType());
        request.setPriority(post.getPriority());
        request.setTargetType(post.getTargetType());
        request.setTargetRole(post.getTargetRole());
        request.setTargetShiftId(post.getTargetShiftId());
        request.setPinned(post.getIsPinned());
        exec.diskIO().execute(() -> {
            try {
                retrofit2.Response<NewsPostResponse> response = apiService.createPost(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    refreshPosts();
                    exec.mainThread().execute(onSuccess);
                } else {
                    exec.mainThread().execute(onError);
                }
            } catch (Exception e) {
                exec.mainThread().execute(onError);
            }
        });
    }

    public void updatePost(NewsPostEntity post, Runnable onSuccess) {
        UpdateNewsRequest request = new UpdateNewsRequest();
        request.setTitle(post.getTitle());
        request.setContent(post.getContent());
        request.setType(post.getType());
        request.setPriority(post.getPriority());
        request.setTargetType(post.getTargetType());
        request.setTargetRole(post.getTargetRole());
        request.setTargetShiftId(post.getTargetShiftId());
        request.setIsPinned(post.getIsPinned());
        exec.diskIO().execute(() -> {
            try {
                retrofit2.Response<NewsPostResponse> response = apiService.updatePost(post.getPostId(), request).execute();
                if (response.isSuccessful()) {
                    refreshPosts();
                    exec.mainThread().execute(onSuccess);
                } else {
                    exec.mainThread().execute(onSuccess);
                }
            } catch (Exception e) {
                exec.mainThread().execute(onSuccess);
            }
        });
    }

    public void deletePost(int postId) {
        exec.diskIO().execute(() -> {
            try {
                apiService.deletePost(postId).execute();
                refreshPosts();
            } catch (Exception e) {
                // Log error
            }
        });
    }

    public void markPostRead(int postId, int userId) {
        exec.diskIO().execute(() -> {
            try {
                retrofit2.Response<Void> response = apiService.markRead(postId).execute();
                if (response.isSuccessful()) {
                    refreshPosts();
                }
            } catch (Exception e) {
                // Log error
            }
        });
    }

    public NewsPostEntity getPostById(int postId) {
        try {
            retrofit2.Response<NewsPostResponse> response = apiService.getPostById(postId).execute();
            if (response.isSuccessful() && response.body() != null) {
                return mapResponseToEntity(response.body());
            }
        } catch (Exception e) {
            // Log error
        }
        return null;
    }

    // Refresh cache from server
    public void refreshPosts() {
        exec.diskIO().execute(() -> {
            try {
                retrofit2.Response<List<NewsPostResponse>> response = apiService.getAllPosts().execute();
                if (response.isSuccessful() && response.body() != null) {
                    List<NewsPostEntity> entities = new ArrayList<>();
                    for (NewsPostResponse r : response.body()) {
                        entities.add(mapResponseToEntity(r));
                    }
                    allPostsCache.postValue(entities);
                }
            } catch (Exception e) {
                // Log error
            }
        });
    }

    private NewsPostEntity mapResponseToEntity(NewsPostResponse r) {
        NewsPostEntity e = new NewsPostEntity();
        e.setPostId(r.getPostId());
        e.setTitle(r.getTitle());
        e.setContent(r.getContent());
        e.setType(r.getType());
        e.setPriority(r.getPriority());
        e.setTargetType(r.getTargetType());
        e.setTargetRole(r.getTargetRole());
        e.setTargetShiftId(r.getTargetShiftId());
        e.setCreatedByUserId(r.getCreatedByUserId());
        e.setCreatedAt(r.getCreatedAt());
        e.setUpdatedAt(r.getUpdatedAt());
        e.setIsPinned(r.getIsPinned());
        e.setIsDeleted(r.getIsDeleted());
        e.setIsRead(r.getIsRead());
        return e;
    }
}