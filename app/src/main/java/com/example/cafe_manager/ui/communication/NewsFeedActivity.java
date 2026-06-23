package com.example.cafe_manager.ui.communication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.entity.NewsPostEntity;
import com.example.cafe_manager.data.local.entity.ShiftAssignmentEntity;
import com.example.cafe_manager.data.local.entity.UserEntity;
import com.example.cafe_manager.manager.SessionManager;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.viewmodel.NewsViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NewsFeedActivity extends AppCompatActivity {

    private NewsViewModel viewModel;
    private NewsFeedAdapter adapter;
    private NewsFilterAdapter filterAdapter;
    private AppDatabase db;

    private List<NewsPostEntity> originalPosts = new ArrayList<>();
    private final Set<Integer> unreadPostIds = new HashSet<>();
    private final Set<Integer> assignedShiftIds = new HashSet<>();
    private final Map<Integer, String> userNamesMap = new HashMap<>();

    private int currentUserId;
    private TextView caption;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_news_feed);

        db = AppDatabase.getInstance(this);
        currentUserId = SessionManager.getInstance(this).getUserId();

        setupTopBar();
        setupFilters();
        setupRecyclerView();
        setupViewModel();
    }

    private void setupTopBar() {
        View topBar = findViewById(R.id.top_bar);
        TextView title = topBar.findViewById(R.id.tv_title);
        caption = topBar.findViewById(R.id.tv_caption);
        View btnBack = topBar.findViewById(R.id.btn_back);
        ImageButton btnRight = topBar.findViewById(R.id.btn_right);

        title.setText("Bảng tin chung");
        caption.setText("Thông báo chính thức từ quản lý");
        btnBack.setOnClickListener(v -> finish());

        // Display add button only for ADMIN and MANAGER
        String role = SessionManager.getInstance(this).getRole();
        if (Constants.ROLE_ADMIN.equals(role) || Constants.ROLE_MANAGER.equals(role)) {
            btnRight.setVisibility(View.VISIBLE);
            btnRight.setImageResource(R.drawable.ic_plus);
            btnRight.setOnClickListener(v -> {
                Intent intent = new Intent(this, NewsPostFormActivity.class);
                startActivity(intent);
            });
        } else {
            btnRight.setVisibility(View.GONE);
        }
    }

    private void setupFilters() {
        RecyclerView rvFilters = findViewById(R.id.rv_filters);
        rvFilters.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        filterAdapter = new NewsFilterAdapter(filter -> {
            filterAdapter.setSelectedFilter(filter);
            filterAndDisplayPosts();
        });
        rvFilters.setAdapter(filterAdapter);
    }

    private void setupRecyclerView() {
        RecyclerView rvNews = findViewById(R.id.rv_news);
        rvNews.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NewsFeedAdapter(post -> {
            // Mark read in DB
            viewModel.markRead(post.getPostId(), currentUserId);

            // Open detail activity
            Intent intent = new Intent(this, NewsDetailActivity.class);
            intent.putExtra("post_id", post.getPostId());
            startActivity(intent);
        });
        rvNews.setAdapter(adapter);

        // Configure empty state text
        View emptyState = findViewById(R.id.empty_state);
        TextView emptyMsg = emptyState.findViewById(R.id.tv_empty_message);
        emptyMsg.setText("Chưa có thông báo nào phù hợp.");
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(NewsViewModel.class);

        // Observe unread count for caption
        viewModel.getUnreadCount(currentUserId).observe(this, count -> {
            if (count != null && count > 0) {
                caption.setText("Bảng tin chung (" + count + " chưa đọc)");
            } else {
                caption.setText("Bảng tin chung");
            }
        });

        // Observe posts
        viewModel.getVisiblePosts().observe(this, posts -> {
            if (posts != null) {
                originalPosts = posts;
                filterAndDisplayPosts();
            }
        });

        // Observe unreads
        viewModel.getUnreadPosts(currentUserId).observe(this, unreads -> {
            unreadPostIds.clear();
            if (unreads != null) {
                for (NewsPostEntity p : unreads) {
                    unreadPostIds.add(p.getPostId());
                }
            }
            adapter.setUnreadPostIds(unreadPostIds);
            filterAndDisplayPosts();
        });

        // Observe employee names
        db.userDao().getAllUsers().observe(this, users -> {
            if (users != null) {
                userNamesMap.clear();
                for (UserEntity u : users) {
                    userNamesMap.put(u.getUserId(), u.getFullName());
                }
                adapter.setUserNames(userNamesMap);
            }
        });

        // Observe shift assignments to know which shift posts are visible to this user
        db.shiftAssignmentDao().getByUser(currentUserId).observe(this, assignments -> {
            assignedShiftIds.clear();
            if (assignments != null) {
                for (ShiftAssignmentEntity a : assignments) {
                    assignedShiftIds.add(a.getShiftId());
                }
            }
            filterAndDisplayPosts();
        });
    }

    private void filterAndDisplayPosts() {
        List<NewsPostEntity> filtered = new ArrayList<>();
        String role = SessionManager.getInstance(this).getRole();
        boolean isAdminOrManager = Constants.ROLE_ADMIN.equals(role) || Constants.ROLE_MANAGER.equals(role);
        String selectedFilter = filterAdapter.getSelectedFilter();

        for (NewsPostEntity post : originalPosts) {
            // 1. Check targeting rules
            boolean targetMatched = false;
            if (isAdminOrManager) {
                targetMatched = true; // Admins and managers see everything
            } else {
                String targetType = post.getTargetType() != null ? post.getTargetType() : "ALL";
                if ("ALL".equals(targetType)) {
                    targetMatched = true;
                } else if ("ROLE".equals(targetType)) {
                    if (role.equals(post.getTargetRole())) {
                        targetMatched = true;
                    }
                } else if ("SHIFT".equals(targetType)) {
                    if (post.getTargetShiftId() != null && assignedShiftIds.contains(post.getTargetShiftId())) {
                        targetMatched = true;
                    }
                }
            }

            if (!targetMatched) continue;

            // 2. Check selected filter
            boolean filterMatched = false;
            if ("Tất cả".equals(selectedFilter)) {
                filterMatched = true;
            } else if ("Chưa đọc".equals(selectedFilter)) {
                if (unreadPostIds.contains(post.getPostId())) {
                    filterMatched = true;
                }
            } else if ("Đã ghim".equals(selectedFilter)) {
                if (post.getIsPinned()) {
                    filterMatched = true;
                }
            } else if ("Khẩn cấp".equals(selectedFilter)) {
                if ("URGENT".equals(post.getPriority())) {
                    filterMatched = true;
                }
            }

            if (filterMatched) {
                filtered.add(post);
            }
        }

        adapter.submitList(filtered);
        boolean isEmpty = filtered.isEmpty();
        findViewById(R.id.rv_news).setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        findViewById(R.id.empty_state).setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }
}
