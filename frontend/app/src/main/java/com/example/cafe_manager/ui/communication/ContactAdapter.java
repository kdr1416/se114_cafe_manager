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
import com.example.cafe_manager.data.local.entity.UserEntity;

public class ContactAdapter extends ListAdapter<UserEntity, ContactAdapter.ViewHolder> {

    public interface OnContactClickListener {
        void onContactClick(UserEntity user);
    }

    private final OnContactClickListener listener;

    public ContactAdapter(OnContactClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<UserEntity> DIFF_CALLBACK = new DiffUtil.ItemCallback<UserEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull UserEntity oldItem, @NonNull UserEntity newItem) {
            return oldItem.getUserId() == newItem.getUserId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull UserEntity oldItem, @NonNull UserEntity newItem) {
            return oldItem.getUserId() == newItem.getUserId()
                    && safeEquals(oldItem.getFullName(), newItem.getFullName())
                    && safeEquals(oldItem.getRole(), newItem.getRole());
        }

        private boolean safeEquals(String a, String b) {
            return a == null ? b == null : a.equals(b);
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvAvatar;
        private final TextView tvName;
        private final TextView tvRole;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tv_contact_avatar);
            tvName = itemView.findViewById(R.id.tv_contact_name);
            tvRole = itemView.findViewById(R.id.tv_contact_role);
        }

        void bind(UserEntity user) {
            tvName.setText(user.getFullName());

            // Avatar: first letter
            if (user.getFullName() != null && !user.getFullName().trim().isEmpty()) {
                String firstChar = user.getFullName().trim().substring(0, 1).toUpperCase();
                tvAvatar.setText(firstChar);
            } else {
                tvAvatar.setText("👤");
            }

            // Role label
            String roleStr = user.getRole();
            if (roleStr == null) roleStr = "";
            switch (roleStr.toUpperCase()) {
                case "ADMIN":
                    tvRole.setText("Quản trị viên");
                    break;
                case "MANAGER":
                    tvRole.setText("Quản lý");
                    break;
                case "STAFF":
                    tvRole.setText("Nhân viên");
                    break;
                default:
                    tvRole.setText(roleStr);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onContactClick(user);
                }
            });
        }
    }
}
