package com.example.cafe_manager.ui.communication;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.cafe_manager.R;
import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.entity.NewsPostEntity;
import com.example.cafe_manager.data.local.entity.ShiftEntity;
import com.example.cafe_manager.data.local.entity.UserEntity;
import com.example.cafe_manager.manager.SessionManager;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.RepositoryCallback;
import com.example.cafe_manager.viewmodel.NewsViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NewsDetailActivity extends AppCompatActivity {

    private int postId;
    private NewsViewModel viewModel;
    private AppDatabase db;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private final SimpleDateFormat shiftDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private TextView tvDetailPriority;
    private TextView tvDetailType;
    private TextView tvDetailPinned;
    private TextView tvDetailTitle;
    private TextView tvDetailMeta;
    private TextView tvDetailContent;
    private TextView tvDetailTarget;
    private View layoutAdminActions;
    private Button btnDetailEdit;
    private Button btnDetailDelete;

    private NewsPostEntity currentPost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_news_detail);

        db = AppDatabase.getInstance(this);
        postId = getIntent().getIntExtra("post_id", -1);
        if (postId == -1) {
            Toast.makeText(this, "Không tìm thấy ID thông báo", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupTopBar();
        initViews();
        setupViewModel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPostDetails();
    }

    private void setupTopBar() {
        View topBar = findViewById(R.id.top_bar);
        TextView title = topBar.findViewById(R.id.tv_title);
        TextView caption = topBar.findViewById(R.id.tv_caption);
        View btnBack = topBar.findViewById(R.id.btn_back);
        
        title.setText("Chi tiết thông báo");
        caption.setText("Đọc nội dung thông báo đầy đủ");
        btnBack.setOnClickListener(v -> finish());
    }

    private void initViews() {
        tvDetailPriority = findViewById(R.id.tv_detail_priority);
        tvDetailType = findViewById(R.id.tv_detail_type);
        tvDetailPinned = findViewById(R.id.tv_detail_pinned);
        tvDetailTitle = findViewById(R.id.tv_detail_title);
        tvDetailMeta = findViewById(R.id.tv_detail_meta);
        tvDetailContent = findViewById(R.id.tv_detail_content);
        tvDetailTarget = findViewById(R.id.tv_detail_target);
        layoutAdminActions = findViewById(R.id.layout_admin_actions);
        btnDetailEdit = findViewById(R.id.btn_detail_edit);
        btnDetailDelete = findViewById(R.id.btn_detail_delete);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(NewsViewModel.class);

        viewModel.getMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                viewModel.clearMessage();
            }
        });
    }

    private void loadPostDetails() {
        viewModel.getPostById(postId, new RepositoryCallback<NewsPostEntity>() {
            @Override
            public void onSuccess(NewsPostEntity post) {
                currentPost = post;
                displayPost(post);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(NewsDetailActivity.this, "Lỗi khi tải thông báo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayPost(NewsPostEntity post) {
        tvDetailTitle.setText(post.getTitle());
        tvDetailContent.setText(post.getContent());

        // Pinned
        tvDetailPinned.setVisibility(post.getIsPinned() ? View.VISIBLE : View.GONE);

        // Priority
        String priority = post.getPriority() != null ? post.getPriority() : "NORMAL";
        tvDetailPriority.setText(translatePriority(priority));
        if ("URGENT".equals(priority)) {
            tvDetailPriority.setBackgroundResource(R.drawable.bg_badge_warning);
            tvDetailPriority.setTextColor(getColor(R.color.error));
        } else if ("IMPORTANT".equals(priority)) {
            tvDetailPriority.setBackgroundResource(R.drawable.bg_badge_warning);
            tvDetailPriority.setTextColor(getColor(R.color.warning));
        } else {
            tvDetailPriority.setBackgroundResource(R.drawable.bg_badge_accent);
            tvDetailPriority.setTextColor(getColor(R.color.accent_dark));
        }

        // Type
        String type = post.getType() != null ? post.getType() : "GENERAL";
        tvDetailType.setText(translateType(type));
        if ("URGENT".equals(type) || "RULE".equals(type)) {
            tvDetailType.setBackgroundResource(R.drawable.bg_badge_warning);
            tvDetailType.setTextColor(getColor(R.color.warning));
        } else if ("GENERAL".equals(type) || "PROMOTION".equals(type)) {
            tvDetailType.setBackgroundResource(R.drawable.bg_badge_success);
            tvDetailType.setTextColor(getColor(R.color.success));
        } else {
            tvDetailType.setBackgroundResource(R.drawable.bg_badge_accent);
            tvDetailType.setTextColor(getColor(R.color.accent_dark));
        }

        // Target Info
        String targetType = post.getTargetType() != null ? post.getTargetType() : "ALL";
        if ("ALL".equals(targetType)) {
            tvDetailTarget.setText("Đối tượng: Tất cả nhân viên");
        } else if ("ROLE".equals(targetType)) {
            tvDetailTarget.setText("Đối tượng: Nhóm vai trò " + translateRole(post.getTargetRole()));
        } else if ("SHIFT".equals(targetType)) {
            if (post.getTargetShiftId() != null) {
                AppExecutors.getInstance().diskIO().execute(() -> {
                    ShiftEntity shift = db.shiftDao().getById(post.getTargetShiftId());
                    if (shift != null) {
                        String shiftDateStr = shiftDateFormat.format(new java.util.Date(shift.getShiftDate()));
                        String shiftInfo = "Đối tượng: Ca làm " + shift.getShiftName() 
                                + " (" + shiftDateStr + " " + shift.getStartTime() + " - " + shift.getEndTime() + ")";
                        AppExecutors.getInstance().mainThread().execute(() -> tvDetailTarget.setText(shiftInfo));
                    }
                });
            } else {
                tvDetailTarget.setText("Đối tượng: Ca làm việc");
            }
        }

        // Creator and Date Info
        AppExecutors.getInstance().diskIO().execute(() -> {
            UserEntity creator = db.userDao().getById(post.getCreatedByUserId());
            AppExecutors.getInstance().mainThread().execute(() -> {
                String creatorName = creator != null ? creator.getFullName() : "Người dùng #" + post.getCreatedByUserId();
                String dateStr = dateFormat.format(new java.util.Date(post.getCreatedAt()));
                String metaText = "Đăng bởi: " + creatorName + " • " + dateStr;
                if (post.getUpdatedAt() != null) {
                    metaText += " (Đã sửa lúc " + dateFormat.format(new java.util.Date(post.getUpdatedAt())) + ")";
                }
                tvDetailMeta.setText(metaText);
            });
        });

        // Edit/Delete options visibility check
        String currentRole = SessionManager.getInstance(this).getRole();
        int currentUserId = SessionManager.getInstance(this).getUserId();
        boolean hasEditPermission = Constants.ROLE_ADMIN.equals(currentRole) 
                || Constants.ROLE_MANAGER.equals(currentRole) 
                || currentUserId == post.getCreatedByUserId();

        if (hasEditPermission) {
            layoutAdminActions.setVisibility(View.VISIBLE);
            btnDetailEdit.setOnClickListener(v -> {
                Intent intent = new Intent(this, NewsPostFormActivity.class);
                intent.putExtra("post_id", post.getPostId());
                startActivity(intent);
            });

            btnDetailDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Xóa thông báo")
                        .setMessage("Bạn có chắc chắn muốn xóa thông báo này? Thao tác này không thể hoàn tác.")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            viewModel.deletePost(post.getPostId());
                            finish();
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            });
        } else {
            layoutAdminActions.setVisibility(View.GONE);
        }
    }

    private String translatePriority(String priority) {
        switch (priority) {
            case "URGENT": return "KHẨN CẤP";
            case "IMPORTANT": return "QUAN TRỌNG";
            default: return "THƯỜNG";
        }
    }

    private String translateType(String type) {
        switch (type) {
            case "MEETING": return "HỌP";
            case "SHIFT": return "CA LÀM";
            case "RULE": return "NỘI QUY";
            case "URGENT": return "KHẨN";
            case "PROMOTION": return "KHUYẾN MÃI";
            case "STOCK": return "KHO HÀNG";
            default: return "CHUNG";
        }
    }

    private String translateRole(String role) {
        if (Constants.ROLE_ADMIN.equals(role)) return "Admin";
        if (Constants.ROLE_MANAGER.equals(role)) return "Quản lý";
        if (Constants.ROLE_STAFF.equals(role)) return "Nhân viên";
        return role;
    }
}
