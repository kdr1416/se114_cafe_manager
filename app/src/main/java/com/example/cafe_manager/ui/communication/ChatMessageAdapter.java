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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ChatMessageAdapter extends ListAdapter<ChatMessageEntity, RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_RECEIVED = 0;
    private static final int VIEW_TYPE_SENT = 1;

    private final int currentUserId;
    private final Map<Integer, String> userNames = new HashMap<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public ChatMessageAdapter(int currentUserId) {
        super(DIFF_CALLBACK);
        this.currentUserId = currentUserId;
    }

    public void setUserNames(Map<Integer, String> names) {
        if (names != null) {
            userNames.clear();
            userNames.putAll(names);
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessageEntity msg = getItem(position);
        if (msg.getSenderId() == currentUserId) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    private static final DiffUtil.ItemCallback<ChatMessageEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ChatMessageEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull ChatMessageEntity oldItem, @NonNull ChatMessageEntity newItem) {
                    return oldItem.getMessageId() == newItem.getMessageId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull ChatMessageEntity oldItem, @NonNull ChatMessageEntity newItem) {
                    return oldItem.getMessageId() == newItem.getMessageId()
                            && oldItem.getRoomId() == newItem.getRoomId()
                            && oldItem.getSenderId() == newItem.getSenderId()
                            && oldItem.getCreatedAt() == newItem.getCreatedAt()
                            && safeEquals(oldItem.getContent(), newItem.getContent());
                }

                private boolean safeEquals(String a, String b) {
                    return a == null ? b == null : a.equals(b);
                }
            };

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_message_sent, parent, false);
            return new SentMsgVH(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_message_received, parent, false);
            return new ReceivedMsgVH(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessageEntity msg = getItem(position);
        if (holder instanceof SentMsgVH) {
            ((SentMsgVH) holder).bind(msg);
        } else if (holder instanceof ReceivedMsgVH) {
            ((ReceivedMsgVH) holder).bind(msg);
        }
    }

    class SentMsgVH extends RecyclerView.ViewHolder {
        private final TextView tvBody;
        private final TextView tvTime;

        SentMsgVH(@NonNull View itemView) {
            super(itemView);
            tvBody = itemView.findViewById(R.id.tv_message_body);
            tvTime = itemView.findViewById(R.id.tv_message_time);
        }

        void bind(ChatMessageEntity msg) {
            tvBody.setText(msg.getContent());
            tvTime.setText(timeFormat.format(new Date(msg.getCreatedAt())));
        }
    }

    class ReceivedMsgVH extends RecyclerView.ViewHolder {
        private final TextView tvSender;
        private final TextView tvBody;
        private final TextView tvTime;

        ReceivedMsgVH(@NonNull View itemView) {
            super(itemView);
            tvSender = itemView.findViewById(R.id.tv_message_sender);
            tvBody = itemView.findViewById(R.id.tv_message_body);
            tvTime = itemView.findViewById(R.id.tv_message_time);
        }

        void bind(ChatMessageEntity msg) {
            String name = userNames.get(msg.getSenderId());
            tvSender.setText(name != null ? name : "Nhân viên #" + msg.getSenderId());
            tvBody.setText(msg.getContent());
            tvTime.setText(timeFormat.format(new Date(msg.getCreatedAt())));
        }
    }
}
