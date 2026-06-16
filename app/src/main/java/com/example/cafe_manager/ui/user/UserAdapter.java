package com.example.cafe_manager.ui.user;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.data.local.entity.UserEntity;
import com.example.cafe_manager.util.Constants;

public class UserAdapter extends ListAdapter<UserEntity, UserAdapter.ViewHolder> {

    public interface OnActionListener {
        void onEdit(UserEntity user);
        void onToggleActive(UserEntity user);
        void onResetPassword(UserEntity user);
    }

    private final OnActionListener listener;

    public UserAdapter(OnActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<UserEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<UserEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull UserEntity oldItem, @NonNull UserEntity newItem) {
                    return oldItem.getUserId() == newItem.getUserId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull UserEntity oldItem, @NonNull UserEntity newItem) {
                    return oldItem.getUserId() == newItem.getUserId()
                            && oldItem.isActive() == newItem.isActive()
                            && safeEquals(oldItem.getFullName(), newItem.getFullName())
                            && safeEquals(oldItem.getRole(), newItem.getRole())
                            && safeEquals(oldItem.getPhone(), newItem.getPhone());
                }

                private boolean safeEquals(String a, String b) {
                    return a == null ? b == null : a.equals(b);
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvFullName;
        private final TextView tvUsername;
        private final TextView tvRole;
        private final TextView tvStatus;
        private final ImageButton btnEdit;
        private final ImageButton btnToggleActive;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFullName = itemView.findViewById(R.id.tv_full_name);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvRole = itemView.findViewById(R.id.tv_role);
            tvStatus = itemView.findViewById(R.id.tv_status);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnToggleActive = itemView.findViewById(R.id.btn_toggle_active);
        }

        void bind(UserEntity user) {
            tvFullName.setText(user.getFullName());
            tvUsername.setText("@" + user.getUsername());

            // Role badge
            tvRole.setText(user.getRole());
            if (Constants.ROLE_ADMIN.equals(user.getRole())) {
                tvRole.setBackgroundResource(R.drawable.bg_badge_accent);
            } else if (Constants.ROLE_MANAGER.equals(user.getRole())) {
                tvRole.setBackgroundResource(R.drawable.bg_badge_warning);
            } else {
                tvRole.setBackgroundResource(R.drawable.bg_badge_success);
            }

            // Status badge
            if (user.isActive()) {
                tvStatus.setText("Hoạt động");
                tvStatus.setBackgroundResource(R.drawable.bg_badge_success);
            } else {
                tvStatus.setText("Đã khóa");
                tvStatus.setBackgroundResource(R.drawable.bg_badge_warning);
            }

            // Toggle active icon
            btnToggleActive.setImageResource(
                    user.isActive() ? R.drawable.ic_eye : R.drawable.ic_eye_off);

            btnEdit.setOnClickListener(v -> listener.onEdit(user));
            btnToggleActive.setOnClickListener(v -> listener.onToggleActive(user));
            itemView.setOnLongClickListener(v -> {
                listener.onResetPassword(user);
                return true;
            });
        }
    }
}
