package com.example.cafe_manager.ui.communication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.entity.ChatMessageEntity;
import com.example.cafe_manager.data.local.entity.ChatParticipantEntity;
import com.example.cafe_manager.data.local.entity.UserEntity;
import com.example.cafe_manager.manager.SessionManager;
import com.example.cafe_manager.viewmodel.ChatViewModel;

import java.util.HashMap;
import java.util.Map;

public class ChatMessageActivity extends AppCompatActivity {

    private int roomId;
    private String roomName;
    private ChatViewModel viewModel;
    private ChatMessageAdapter adapter;
    private AppDatabase db;
    private int currentUserId;

    private RecyclerView rvChatMessages;
    private EditText etMessageInput;
    private Button btnMessageSend;
    private View emptyState;

    private boolean isFirstLoad = true;
    private boolean isNearBottom = true;
    private final Map<Integer, String> userNamesMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat_message);

        db = AppDatabase.getInstance(this);
        currentUserId = SessionManager.getInstance(this).getUserId();

        roomId = getIntent().getIntExtra("room_id", -1);
        roomName = getIntent().getStringExtra("room_name");

        if (roomId == -1) {
            Toast.makeText(this, "Không tìm thấy phòng chat", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Check participant access
        ChatParticipantEntity participant = db.chatParticipantDao().getByRoomAndUser(roomId, currentUserId);
        if (participant == null) {
            Toast.makeText(this, "Bạn không có quyền truy cập phòng chat này", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupTopBar();
        setupRecyclerView();
        setupViewModel();
    }

    private void initViews() {
        rvChatMessages = findViewById(R.id.rv_chat_messages);
        etMessageInput = findViewById(R.id.et_message_input);
        btnMessageSend = findViewById(R.id.btn_message_send);
        emptyState = findViewById(R.id.empty_state);

        // Set empty state message
        TextView emptyMessage = emptyState.findViewById(R.id.tv_empty_message);
        emptyMessage.setText("Chưa có tin nhắn nào.\nHãy bắt đầu cuộc trò chuyện!");

        etMessageInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                btnMessageSend.performClick();
                return true;
            }
            return false;
        });
    }

    private void setupTopBar() {
        View topBar = findViewById(R.id.top_bar);
        TextView title = topBar.findViewById(R.id.tv_title);
        TextView caption = topBar.findViewById(R.id.tv_caption);
        View btnBack = topBar.findViewById(R.id.btn_back);

        title.setText(roomName != null ? roomName : "Phòng chat");
        caption.setVisibility(View.GONE);
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        rvChatMessages.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatMessageAdapter(currentUserId);
        rvChatMessages.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        // Observe messages in room (last 200 messages)
        viewModel.getMessages(roomId, 200).observe(this, messages -> {
            if (messages != null) {
                adapter.submitList(messages);

                // Show empty state if no messages
                boolean isEmpty = messages.isEmpty();
                rvChatMessages.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

                if (!isEmpty) {
                    // Bulk mark all received messages in this room as read
                    viewModel.markAllMessagesRead(roomId, currentUserId);

                    // Smart scroll: only scroll if user is near bottom (within 100px)
                    rvChatMessages.post(() -> {
                        RecyclerView.LayoutManager lm = rvChatMessages.getLayoutManager();
                        if (lm instanceof LinearLayoutManager) {
                            LinearLayoutManager llm = (LinearLayoutManager) lm;
                            int lastVisible = llm.findLastCompletelyVisibleItemPosition();
                            int totalCount = adapter.getItemCount();

                            // Calculate if near bottom (last visible item is within threshold)
                            boolean shouldScroll = isFirstLoad || (totalCount - lastVisible <= 5);

                            if (shouldScroll && totalCount > 0) {
                                if (isFirstLoad) {
                                    rvChatMessages.scrollToPosition(totalCount - 1);
                                    isFirstLoad = false;
                                } else {
                                    rvChatMessages.smoothScrollToPosition(totalCount - 1);
                                }
                            }
                        }
                    });
                }
            }
        });

        // Observe user names to display receiver sender names
        db.userDao().getAllUsers().observe(this, users -> {
            if (users != null) {
                userNamesMap.clear();
                for (UserEntity u : users) {
                    userNamesMap.put(u.getUserId(), u.getFullName());
                }
                adapter.setUserNames(userNamesMap);
            }
        });

        // Observe error/info messages
        viewModel.getMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                viewModel.clearMessage();
            }
        });

        // Setup send click
        btnMessageSend.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String content = etMessageInput.getText().toString().trim();
        if (content.isEmpty()) {
            return;
        }

        // Disable button during send
        btnMessageSend.setEnabled(false);
        btnMessageSend.setAlpha(0.5f);

        viewModel.sendMessage(roomId, currentUserId, content, () -> {
            etMessageInput.setText("");
            // Re-enable button
            btnMessageSend.setEnabled(true);
            btnMessageSend.setAlpha(1.0f);
        }, () -> {
            // Error occurred
            Toast.makeText(ChatMessageActivity.this, "Gửi tin nhắn thất bại", Toast.LENGTH_SHORT).show();
            // Re-enable button
            btnMessageSend.setEnabled(true);
            btnMessageSend.setAlpha(1.0f);
        });
    }
}
