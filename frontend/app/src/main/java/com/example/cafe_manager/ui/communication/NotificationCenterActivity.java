package com.example.cafe_manager.ui.communication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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

public class NotificationCenterActivity extends AppCompatActivity {

    private NewsViewModel viewModel;
    private NewsFeedAdapter adapter;
    private AppDatabase db;

    private List<NewsPostEntity> unreadPostsList = new ArrayList<>();
    private final Set<Integer> assignedShiftIds = new HashSet<>();
    private final Map<Integer, String> userNamesMap = new HashMap<>();

    private int currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notification_center);

        db = AppDatabase.getInstance(this);
        currentUserId = SessionManager.getInstance(this).getUserId();

        setupTopBar();
        setupRecyclerView();
        setupViewModel();
    }

    private void setupTopBar() {
        View topBar = findViewById(R.id.top_bar);
        TextView title = topBar.findViewById(R.id.tv_title);
        TextView caption = topBar.findViewById(R.id.tv_caption);
        View btnBack = topBar.findViewById(R.id.btn_back);

        title.setText("Thông báo");
        caption.setText("Bạn không có thông báo mới");
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        RecyclerView rvNotifications = findViewById(R.id.rv_notifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new NewsFeedAdapter(post -> {
            // Mark read in DB
            viewModel.markRead(post.getPostId(), currentUserId);

            // Open detail activity
            Intent intent = new Intent(this, NewsDetailActivity.class);
            intent.putExtra("post_id", post.getPostId());
            startActivity(intent);
        });
        rvNotifications.setAdapter(adapter);

        // Configure empty state text
        View emptyState = findViewById(R.id.empty_state);
        TextView emptyMsg = emptyState.findViewById(R.id.tv_empty_message);
        emptyMsg.setText("Bạn đã đọc hết tất cả thông báo.");
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(NewsViewModel.class);

        // Observe unreads
        viewModel.getUnreadPosts(currentUserId).observe(this, unreads -> {
            if (unreads != null) {
                unreadPostsList = unreads;
                
                // We mark all of them as unread for the adapter to display unread dots
                Set<Integer> unreadIds = new HashSet<>();
                for (NewsPostEntity p : unreads) {
                    unreadIds.add(p.getPostId());
                }
                adapter.setUnreadPostIds(unreadIds);

                filterAndDisplayNotifications();
            }
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
            filterAndDisplayNotifications();
        });
    }

    private void filterAndDisplayNotifications() {
        List<NewsPostEntity> filtered = new ArrayList<>();
        String role = SessionManager.getInstance(this).getRole();
        boolean isAdminOrManager = Constants.ROLE_ADMIN.equals(role) || Constants.ROLE_MANAGER.equals(role);

        for (NewsPostEntity post : unreadPostsList) {
            // Check targeting rules
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

            if (targetMatched) {
                filtered.add(post);
            }
        }

        adapter.submitList(filtered);
        
        // Update top bar caption count
        int count = filtered.size();
        TextView caption = findViewById(R.id.top_bar).findViewById(R.id.tv_caption);
        if (caption != null) {
            if (count > 0) {
                caption.setText("Bạn có " + count + " thông báo chưa đọc");
            } else {
                caption.setText("Bạn không có thông báo mới");
            }
        }

        boolean isEmpty = filtered.isEmpty();
        findViewById(R.id.rv_notifications).setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        findViewById(R.id.empty_state).setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }
}
