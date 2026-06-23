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
import com.example.cafe_manager.data.local.entity.ChatMessageEntity;
import com.example.cafe_manager.data.local.entity.ChatRoomEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ChatRoomAdapter extends ListAdapter<ChatRoomEntity, ChatRoomAdapter.RoomVH> {

    public interface OnRoomClickListener {
        void onRoomClick(ChatRoomEntity room);
    }

    private final OnRoomClickListener listener;
    private final Map<Integer, Integer> unreadCounts = new HashMap<>();
    private final Map<Integer, ChatMessageEntity> latestMessages = new HashMap<>();
    private final Map<Integer, String> userNames = new HashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat dateDayFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());

    public ChatRoomAdapter(OnRoomClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void setUnreadCounts(Map<Integer, Integer> counts) {
        if (counts != null) {
            unreadCounts.clear();
            unreadCounts.putAll(counts);
            notifyDataSetChanged();
        }
    }

    public void setLatestMessages(Map<Integer, ChatMessageEntity> messages) {
        if (messages != null) {
            latestMessages.clear();
            latestMessages.putAll(messages);
            notifyDataSetChanged();
        }
    }

    public void setUserNames(Map<Integer, String> names) {
        if (names != null) {
            userNames.clear();
            userNames.putAll(names);
            notifyDataSetChanged();
        }
    }

    private static final DiffUtil.ItemCallback<ChatRoomEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ChatRoomEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull ChatRoomEntity oldItem, @NonNull ChatRoomEntity newItem) {
                    return oldItem.getRoomId() == newItem.getRoomId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull ChatRoomEntity oldItem, @NonNull ChatRoomEntity newItem) {
                    return oldItem.getRoomId() == newItem.getRoomId()
                            && oldItem.getIsActive() == newItem.getIsActive()
                            && oldItem.getUpdatedAt() == newItem.getUpdatedAt()
                            && safeEquals(oldItem.getRoomName(), newItem.getRoomName())
                            && safeEquals(oldItem.getRoomType(), newItem.getRoomType());
                }

                private boolean safeEquals(String a, String b) {
                    return a == null ? b == null : a.equals(b);
                }
            };

    @NonNull
    @Override
    public RoomVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_room, parent, false);
        return new RoomVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomVH holder, int position) {
        holder.bind(getItem(position));
    }

    class RoomVH extends RecyclerView.ViewHolder {
        private final TextView tvRoomAvatar;
        private final TextView tvRoomName;
        private final TextView tvRoomTime;
        private final TextView tvRoomLastMessage;
        private final TextView tvRoomBadge;

        RoomVH(@NonNull View itemView) {
            super(itemView);
            tvRoomAvatar = itemView.findViewById(R.id.tv_room_avatar);
            tvRoomName = itemView.findViewById(R.id.tv_room_name);
            tvRoomTime = itemView.findViewById(R.id.tv_room_time);
            tvRoomLastMessage = itemView.findViewById(R.id.tv_room_last_message);
            tvRoomBadge = itemView.findViewById(R.id.tv_room_badge);
        }

        void bind(ChatRoomEntity room) {
            tvRoomName.setText(room.getRoomName());

            // Avatar placeholder
            String firstChar = room.getRoomName().substring(0, 1).toUpperCase();
            tvRoomAvatar.setText(firstChar);

            // Time & Last message
            ChatMessageEntity lastMsg = latestMessages.get(room.getRoomId());
            if (lastMsg != null) {
                // Time
                long time = lastMsg.getCreatedAt();
                tvRoomTime.setText(formatTime(time));

                // Preview content
                String senderName = userNames.get(lastMsg.getSenderId());
                String senderStr = senderName != null ? senderName : "Nhân viên #" + lastMsg.getSenderId();
                tvRoomLastMessage.setText(senderStr + ": " + lastMsg.getContent());
            } else {
                tvRoomTime.setText(formatTime(room.getUpdatedAt()));
                tvRoomLastMessage.setText("Chưa có tin nhắn nào.");
            }

            // Unread Badge
            Integer unreadCount = unreadCounts.get(room.getRoomId());
            if (unreadCount != null && unreadCount > 0) {
                tvRoomBadge.setVisibility(View.VISIBLE);
                tvRoomBadge.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
            } else {
                tvRoomBadge.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRoomClick(room);
                }
            });
        }

        private String formatTime(long timestamp) {
            Date date = new Date(timestamp);
            Date now = new Date();
            // Simple check if same day
            SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            if (fmt.format(date).equals(fmt.format(now))) {
                return dateFormat.format(date);
            } else {
                return dateDayFormat.format(date);
            }
        }
    }
}
