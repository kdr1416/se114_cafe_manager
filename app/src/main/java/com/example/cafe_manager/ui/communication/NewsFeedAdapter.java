package com.example.cafe_manager.ui.communication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.data.local.entity.NewsPostEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class NewsFeedAdapter extends ListAdapter<NewsPostEntity, NewsFeedAdapter.PostViewHolder> {

    public interface OnPostClickListener {
        void onPostClick(NewsPostEntity post);
    }

    private final OnPostClickListener listener;
    private final Set<Integer> unreadPostIds = new HashSet<>();
    private final Map<Integer, String> userNames = new HashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public NewsFeedAdapter(OnPostClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void setUnreadPostIds(Set<Integer> ids) {
        unreadPostIds.clear();
        if (ids != null) {
            unreadPostIds.addAll(ids);
        }
        notifyDataSetChanged();
    }

    public void setUserNames(Map<Integer, String> names) {
        userNames.clear();
        if (names != null) {
            userNames.putAll(names);
        }
        notifyDataSetChanged();
    }

    private static final DiffUtil.ItemCallback<NewsPostEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<NewsPostEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull NewsPostEntity oldItem, @NonNull NewsPostEntity newItem) {
                    return oldItem.getPostId() == newItem.getPostId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull NewsPostEntity oldItem, @NonNull NewsPostEntity newItem) {
                    return oldItem.getPostId() == newItem.getPostId()
                            && oldItem.getIsPinned() == newItem.getIsPinned()
                            && oldItem.getIsDeleted() == newItem.getIsDeleted()
                            && oldItem.getCreatedAt() == newItem.getCreatedAt()
                            && safeEquals(oldItem.getTitle(), newItem.getTitle())
                            && safeEquals(oldItem.getContent(), newItem.getContent())
                            && safeEquals(oldItem.getType(), newItem.getType())
                            && safeEquals(oldItem.getPriority(), newItem.getPriority());
                }

                private boolean safeEquals(String a, String b) {
                    return a == null ? b == null : a.equals(b);
                }
            };

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_news_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class PostViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvBadgePriority;
        private final TextView tvBadgeType;
        private final TextView tvPinnedIndicator;
        private final View viewUnreadDot;
        private final TextView tvPostTitle;
        private final TextView tvPostSnippet;
        private final TextView tvPostAuthor;
        private final TextView tvPostDate;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBadgePriority = itemView.findViewById(R.id.tv_badge_priority);
            tvBadgeType = itemView.findViewById(R.id.tv_badge_type);
            tvPinnedIndicator = itemView.findViewById(R.id.tv_pinned_indicator);
            viewUnreadDot = itemView.findViewById(R.id.view_unread_dot);
            tvPostTitle = itemView.findViewById(R.id.tv_post_title);
            tvPostSnippet = itemView.findViewById(R.id.tv_post_snippet);
            tvPostAuthor = itemView.findViewById(R.id.tv_post_author);
            tvPostDate = itemView.findViewById(R.id.tv_post_date);
        }

        void bind(NewsPostEntity post) {
            tvPostTitle.setText(post.getTitle());
            tvPostSnippet.setText(post.getContent());

            // Author Name
            String author = userNames.get(post.getCreatedByUserId());
            tvPostAuthor.setText("Bởi: " + (author != null ? author : "Người dùng #" + post.getCreatedByUserId()));

            // Date
            tvPostDate.setText(dateFormat.format(new Date(post.getCreatedAt())));

            // Pinned indicator
            tvPinnedIndicator.setVisibility(post.getIsPinned() ? View.VISIBLE : View.GONE);

            // Unread dot
            boolean isUnread = unreadPostIds.contains(post.getPostId());
            viewUnreadDot.setVisibility(isUnread ? View.VISIBLE : View.GONE);

            // Priority badge styling
            String priority = post.getPriority() != null ? post.getPriority() : "NORMAL";
            tvBadgePriority.setText(translatePriority(priority));
            if ("URGENT".equals(priority)) {
                tvBadgePriority.setBackgroundResource(R.drawable.bg_badge_warning);
                tvBadgePriority.setTextColor(itemView.getContext().getColor(R.color.error));
            } else if ("IMPORTANT".equals(priority)) {
                tvBadgePriority.setBackgroundResource(R.drawable.bg_badge_warning);
                tvBadgePriority.setTextColor(itemView.getContext().getColor(R.color.warning));
            } else {
                tvBadgePriority.setBackgroundResource(R.drawable.bg_badge_accent);
                tvBadgePriority.setTextColor(itemView.getContext().getColor(R.color.accent_dark));
            }

            // Type badge styling
            String type = post.getType() != null ? post.getType() : "GENERAL";
            tvBadgeType.setText(translateType(type));
            if ("URGENT".equals(type) || "RULE".equals(type)) {
                tvBadgeType.setBackgroundResource(R.drawable.bg_badge_warning);
                tvBadgeType.setTextColor(itemView.getContext().getColor(R.color.warning));
            } else if ("GENERAL".equals(type) || "PROMOTION".equals(type)) {
                tvBadgeType.setBackgroundResource(R.drawable.bg_badge_success);
                tvBadgeType.setTextColor(itemView.getContext().getColor(R.color.success));
            } else {
                tvBadgeType.setBackgroundResource(R.drawable.bg_badge_accent);
                tvBadgeType.setTextColor(itemView.getContext().getColor(R.color.accent_dark));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPostClick(post);
                }
            });
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
    }
}
