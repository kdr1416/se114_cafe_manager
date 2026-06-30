package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.local.entity.NewsPostEntity;
import com.example.cafe_manager.data.repository.NewsRepository;

import java.util.List;

public class NewsViewModel extends AndroidViewModel {
    private final NewsRepository repo;
    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<NewsFilter> currentFilter = new MutableLiveData<>(NewsFilter.ALL);
    private final MediatorLiveData<List<NewsPostEntity>> filteredPosts = new MediatorLiveData<>();

    public enum NewsFilter {
        ALL,
        UNREAD,
        PINNED,
        URGENT
    }

    public NewsViewModel(@NonNull Application app) {
        super(app);
        repo = NewsRepository.getInstance(app);
        setupFilterObserver(app);
    }

    private void setupFilterObserver(Application app) {
        filteredPosts.addSource(repo.getVisiblePosts(), posts -> {
            NewsFilter filter = currentFilter.getValue();
            if (filter == NewsFilter.ALL || filter == null) {
                filteredPosts.setValue(posts);
            }
        });

        filteredPosts.addSource(repo.getUnreadPosts(getCurrentUserId(app)), posts -> {
            NewsFilter filter = currentFilter.getValue();
            if (filter == NewsFilter.UNREAD) {
                filteredPosts.setValue(posts);
            }
        });

        filteredPosts.addSource(repo.getPinnedPosts(), posts -> {
            NewsFilter filter = currentFilter.getValue();
            if (filter == NewsFilter.PINNED) {
                filteredPosts.setValue(posts);
            }
        });

        filteredPosts.addSource(repo.getUrgentPosts(), posts -> {
            NewsFilter filter = currentFilter.getValue();
            if (filter == NewsFilter.URGENT) {
                filteredPosts.setValue(posts);
            }
        });
    }

    private int getCurrentUserId(Application app) {
        return com.example.cafe_manager.manager.SessionManager.getInstance(app).getUserId();
    }

    public LiveData<List<NewsPostEntity>> getFilteredPosts() {
        return filteredPosts;
    }

    public void setFilter(NewsFilter filter) {
        if (currentFilter.getValue() != filter) {
            currentFilter.setValue(filter);
        }
    }

    public LiveData<List<NewsPostEntity>> getVisiblePosts() {
        return repo.getVisiblePosts();
    }

    public LiveData<List<NewsPostEntity>> getUnreadPosts(int userId) {
        return repo.getUnreadPosts(userId);
    }

    public LiveData<Integer> getUnreadCount(int userId) {
        return repo.getUnreadCount(userId);
    }

    public LiveData<List<NewsPostEntity>> getPinnedPosts() {
        return repo.getPinnedPosts();
    }

    public LiveData<List<NewsPostEntity>> getUrgentPosts() {
        return repo.getUrgentPosts();
    }

    public void createPost(String title, String content, String type, String priority,
                          String targetType, String targetRole, int targetShiftId, int createdBy, String imageUrl) {
        NewsPostEntity p = new NewsPostEntity();
        p.setTitle(title);
        p.setContent(content);
        p.setType(type);
        p.setPriority(priority);
        p.setTargetType(targetType);
        
        // Clean up target fields based on targetType
        if ("ROLE".equals(targetType)) {
            p.setTargetRole(targetRole);
            p.setTargetShiftId(null);
        } else if ("SHIFT".equals(targetType)) {
            p.setTargetRole(null);
            p.setTargetShiftId(targetShiftId);
        } else {
            p.setTargetRole(null);
            p.setTargetShiftId(null);
        }
        
        p.setCreatedByUserId(createdBy);
        p.setCreatedAt(System.currentTimeMillis());
        p.setIsPinned(false);
        p.setIsDeleted(false);
        p.setImageUrl(imageUrl);

        repo.createPost(p,
            () -> message.setValue("Đã đăng thông báo"),
            () -> message.setValue("Không thể đăng thông báo")
        );
    }

    public void refresh() {
        repo.refreshPosts();
    }

    public void markRead(int postId, int userId) {
        repo.markPostRead(postId, userId);
    }

    public void updatePost(NewsPostEntity post, Runnable onSuccess) {
        repo.updatePost(post, () -> {
            message.setValue("Đã cập nhật thông báo");
            if (onSuccess != null) {
                onSuccess.run();
            }
        });
    }

    public void deletePost(int postId) {
        repo.deletePost(postId);
        message.setValue("Đã xóa thông báo");
    }

    public void getPostById(int postId, com.example.cafe_manager.util.RepositoryCallback<NewsPostEntity> callback) {
        com.example.cafe_manager.util.AppExecutors.getInstance().diskIO().execute(() -> {
            NewsPostEntity post = repo.getPostById(postId);
            com.example.cafe_manager.util.AppExecutors.getInstance().mainThread().execute(() -> {
                if (post != null) {
                    callback.onSuccess(post);
                } else {
                    callback.onError(new Exception("Không tìm thấy thông báo"));
                }
            });
        });
    }

    public LiveData<String> getMessage() { 
        return message; 
    }
    
    public void clearMessage() { 
        message.setValue(null); 
    }
}
