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
    private boolean isLoadingMoreMessages = false;
    private static final int LOAD_MORE_THRESHOLD = 3;
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

        // Check participant access - Server will validate when fetching messages

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
        rvChatMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy >= 0) return; // only trigger when scrolling UP
                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm == null) return;
                int firstVisible = lm.findFirstVisibleItemPosition();
                if (firstVisible <= LOAD_MORE_THRESHOLD && !isLoadingMoreMessages) {
                    isLoadingMoreMessages = true;
                    viewModel.loadMoreMessages(roomId);
                }
            }
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        viewModel.loadMessages(roomId);

        viewModel.getIsLoadingMore().observe(this, loading -> {
            if (loading != null && !loading) {
                isLoadingMoreMessages = false;
            }
        });

        viewModel.getMessagesForRoom(roomId).observe(this, messages -> {
            if (messages != null) {
                int firstVisibleVal = -1;
                RecyclerView.LayoutManager lm = rvChatMessages.getLayoutManager();
                if (lm instanceof LinearLayoutManager) {
                    firstVisibleVal = ((LinearLayoutManager) lm).findFirstVisibleItemPosition();
                }
                final int firstVisible = firstVisibleVal;
                final int prevCount = adapter.getItemCount();

                adapter.submitList(messages, () -> {
                    int newCount = adapter.getItemCount();
                    int offset = newCount - prevCount;
                    if (offset > 0 && firstVisible > 0) {
                        RecyclerView.LayoutManager lm2 = rvChatMessages.getLayoutManager();
                        if (lm2 instanceof LinearLayoutManager) {
                            ((LinearLayoutManager) lm2).scrollToPositionWithOffset(firstVisible + offset, 0);
                        }
                    }
                });

                // Show empty state if no messages
                boolean isEmpty = messages.isEmpty();
                rvChatMessages.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

                if (!isEmpty) {
                    // Bulk mark all received messages in this room as read
                    viewModel.markAllMessagesRead(roomId, currentUserId);

                    // Smart scroll: only scroll if user is near bottom (within 100px)
                    rvChatMessages.post(() -> {
                        RecyclerView.LayoutManager postLm = rvChatMessages.getLayoutManager();
                        if (postLm instanceof LinearLayoutManager) {
                            LinearLayoutManager llm = (LinearLayoutManager) postLm;
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

    @Override
    protected void onResume() {
        super.onResume();
        String token = SessionManager.getInstance(this).getToken();
        if (token != null) {
            viewModel.connectWebSocket(token);
            viewModel.switchRoom(-1, roomId);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        viewModel.unsubscribeFromRoom(roomId);
        viewModel.disconnectWebSocket();
    }
}
