package com.example.cafe_manager.ui.communication;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.entity.ChatRoomEntity;
import com.example.cafe_manager.data.local.entity.UserEntity;
import com.example.cafe_manager.manager.SessionManager;
import com.example.cafe_manager.viewmodel.ChatViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MessengerActivity extends AppCompatActivity {

    private ChatViewModel viewModel;
    private ChatRoomAdapter roomAdapter;
    private ContactAdapter contactAdapter;
    private AppDatabase db;
    private int currentUserId;

    private EditText etSearch;
    private Button btnTabRooms;
    private Button btnTabContacts;
    private RecyclerView rvRooms;
    private RecyclerView rvContacts;
    private TextView tvEmpty;
    private ProgressBar progressLoading;

    // Tablet specific fields
    private boolean isTablet;
    private int currentRoomId = -1;
    private View tvSelectRoomHint;
    private View layoutChatArea;
    private TextView tvActiveRoomName;
    private RecyclerView rvChatMessages;
    private EditText etMessageInput;
    private View btnMessageSend;
    private View rightPaneEmptyState;
    private ChatMessageAdapter messageAdapter;
    private boolean isLoadingMoreMessages = false;
    private static final int LOAD_MORE_THRESHOLD = 3;

    private List<ChatRoomEntity> allRooms = new ArrayList<>();
    private List<UserEntity> allContacts = new ArrayList<>();
    private boolean isRoomsTab = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messenger);

        db = AppDatabase.getInstance(this);
        currentUserId = SessionManager.getInstance(this).getUserId();

        isTablet = findViewById(R.id.container_message_pane) != null;

        initViews();
        setupTopBar();
        setupRecyclerViews();
        setupViewModel();
        setupSearch();
        loadContacts();

        if (isTablet) {
            setupTabletChatPane();
        }
    }

    private void initViews() {
        etSearch = findViewById(R.id.et_search);
        btnTabRooms = findViewById(R.id.btn_tab_rooms);
        btnTabContacts = findViewById(R.id.btn_tab_contacts);
        rvRooms = findViewById(R.id.rv_rooms);
        rvContacts = findViewById(R.id.rv_contacts);
        tvEmpty = findViewById(R.id.tv_empty);
        progressLoading = findViewById(R.id.progressLoading);

        btnTabRooms.setOnClickListener(v -> switchTab(true));
        btnTabContacts.setOnClickListener(v -> switchTab(false));
    }

    private void setupTopBar() {
        View topBar = findViewById(R.id.top_bar);
        TextView tvTitle = topBar.findViewById(R.id.tv_title);
        TextView tvCaption = topBar.findViewById(R.id.tv_caption);
        View btnBack = topBar.findViewById(R.id.btn_back);

        tvTitle.setText("Trò chuyện");
        tvCaption.setVisibility(View.GONE);
        btnBack.setVisibility(View.VISIBLE);
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerViews() {
        rvRooms.setLayoutManager(new LinearLayoutManager(this));
        roomAdapter = new ChatRoomAdapter(currentUserId, room -> {
            if (isTablet) {
                openRoomOnTablet(room);
            } else {
                Intent intent = new Intent(this, ChatMessageActivity.class);
                intent.putExtra("room_id", room.getRoomId());
                intent.putExtra("room_name", room.getRoomName());
                startActivity(intent);
            }
        });
        rvRooms.setAdapter(roomAdapter);

        rvContacts.setLayoutManager(new LinearLayoutManager(this));
        contactAdapter = new ContactAdapter(contact -> {
            Toast.makeText(this, "Chat 1-1 sắp ra mắt", Toast.LENGTH_SHORT).show();
        });
        rvContacts.setAdapter(contactAdapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        // Observe rooms
        viewModel.getRoomsByParticipant(currentUserId).observe(this, rooms -> {
            if (rooms != null) {
                allRooms = rooms;
                filterRooms(etSearch.getText().toString());
                
                // Subscribe to WebSocket for each room to enable real-time previews
                for (ChatRoomEntity r : rooms) {
                    viewModel.subscribeToRoom(r.getRoomId());
                    
                    // Observe latest message
                    viewModel.getLatestMessage(r.getRoomId()).observe(this, msg -> {
                        if (msg != null) {
                            roomAdapter.setLatestMessage(r.getRoomId(), msg);
                        }
                    });

                    // Observe unread counts
                    viewModel.getUnreadCountForRoom(r.getRoomId(), currentUserId).observe(this, count -> {
                        if (count != null) {
                            roomAdapter.setUnreadCount(r.getRoomId(), count);
                        }
                    });
                }
            }
        });

        // Observe user names for room preview
        db.userDao().getAllUsers().observe(this, users -> {
            if (users != null) {
                Map<Integer, String> map = new HashMap<>();
                for (UserEntity u : users) {
                    map.put(u.getUserId(), u.getFullName());
                }
                roomAdapter.setUserNames(map);
            }
        });
    }

    private void setupTabletChatPane() {
        tvSelectRoomHint = findViewById(R.id.tv_select_room_hint);
        layoutChatArea = findViewById(R.id.layout_chat_area);
        tvActiveRoomName = findViewById(R.id.tv_active_room_name);
        rvChatMessages = findViewById(R.id.rv_chat_messages);
        etMessageInput = findViewById(R.id.et_message_input);
        btnMessageSend = findViewById(R.id.btn_message_send);
        rightPaneEmptyState = findViewById(R.id.right_pane_empty_state);

        rvChatMessages.setLayoutManager(new LinearLayoutManager(this));
        messageAdapter = new ChatMessageAdapter(currentUserId);
        rvChatMessages.setAdapter(messageAdapter);

        // Fetch display names in right pane
        db.userDao().getAllUsers().observe(this, users -> {
            if (users != null) {
                Map<Integer, String> map = new HashMap<>();
                for (UserEntity u : users) {
                    map.put(u.getUserId(), u.getFullName());
                }
                messageAdapter.setUserNames(map);
            }
        });

        btnMessageSend.setOnClickListener(v -> sendMessageOnTablet());

        etMessageInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                btnMessageSend.performClick();
                return true;
            }
            return false;
        });

        // Infinite scroll for tablet
        rvChatMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy >= 0) return;
                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm == null) return;
                int firstVisible = lm.findFirstVisibleItemPosition();
                if (firstVisible <= LOAD_MORE_THRESHOLD && !isLoadingMoreMessages && currentRoomId != -1) {
                    isLoadingMoreMessages = true;
                    viewModel.loadMoreMessages(currentRoomId);
                }
            }
        });

        viewModel.getIsLoadingMore().observe(this, loading -> {
            if (loading != null && !loading) {
                isLoadingMoreMessages = false;
            }
        });
    }

    private void openRoomOnTablet(ChatRoomEntity room) {
        tvSelectRoomHint.setVisibility(View.GONE);
        layoutChatArea.setVisibility(View.VISIBLE);
        tvActiveRoomName.setText(room.getRoomName());

        int oldRoomId = currentRoomId;
        currentRoomId = room.getRoomId();

        viewModel.switchRoom(oldRoomId, currentRoomId);
        viewModel.markAllMessagesRead(currentRoomId, currentUserId);

        viewModel.getMessagesForRoom(currentRoomId).removeObservers(this);
        viewModel.getMessagesForRoom(currentRoomId).observe(this, messages -> {
            if (messages != null && currentRoomId == room.getRoomId()) {
                int firstVisibleVal = -1;
                RecyclerView.LayoutManager lm = rvChatMessages.getLayoutManager();
                if (lm instanceof LinearLayoutManager) {
                    firstVisibleVal = ((LinearLayoutManager) lm).findFirstVisibleItemPosition();
                }
                final int firstVisible = firstVisibleVal;
                final int prevCount = messageAdapter.getItemCount();

                messageAdapter.submitList(messages, () -> {
                    int newCount = messageAdapter.getItemCount();
                    int offset = newCount - prevCount;
                    if (offset > 0 && firstVisible > 0) {
                        RecyclerView.LayoutManager lm2 = rvChatMessages.getLayoutManager();
                        if (lm2 instanceof LinearLayoutManager) {
                            ((LinearLayoutManager) lm2).scrollToPositionWithOffset(firstVisible + offset, 0);
                        }
                    }
                });

                boolean isEmpty = messages.isEmpty();
                rvChatMessages.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                if (rightPaneEmptyState != null) {
                    rightPaneEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                    TextView emptyMsg = rightPaneEmptyState.findViewById(R.id.tv_empty_message);
                    if (emptyMsg != null) {
                        emptyMsg.setText("Chưa có tin nhắn nào.\nHãy bắt đầu cuộc trò chuyện!");
                    }
                }

                if (!isEmpty) {
                    viewModel.markAllMessagesRead(currentRoomId, currentUserId);
                }
            }
        });
    }

    private void sendMessageOnTablet() {
        if (currentRoomId == -1) return;
        String content = etMessageInput.getText().toString().trim();
        if (content.isEmpty()) return;

        btnMessageSend.setEnabled(false);
        btnMessageSend.setAlpha(0.5f);

        viewModel.sendMessage(currentRoomId, currentUserId, content, () -> {
            etMessageInput.setText("");
            btnMessageSend.setEnabled(true);
            btnMessageSend.setAlpha(1.0f);
        }, () -> {
            Toast.makeText(this, "Gửi tin nhắn thất bại", Toast.LENGTH_SHORT).show();
            btnMessageSend.setEnabled(true);
            btnMessageSend.setAlpha(1.0f);
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isRoomsTab) {
                    filterRooms(s.toString());
                } else {
                    filterContacts(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void switchTab(boolean isRooms) {
        isRoomsTab = isRooms;
        btnTabRooms.setEnabled(!isRooms);
        btnTabContacts.setEnabled(isRooms);

        if (isRooms) {
            rvRooms.setVisibility(View.VISIBLE);
            rvContacts.setVisibility(View.GONE);
            filterRooms(etSearch.getText().toString());
        } else {
            rvRooms.setVisibility(View.GONE);
            rvContacts.setVisibility(View.VISIBLE);
            filterContacts(etSearch.getText().toString());
        }
    }

    private void filterRooms(String query) {
        if (query == null || query.trim().isEmpty()) {
            roomAdapter.submitList(allRooms);
            tvEmpty.setVisibility(allRooms.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            List<ChatRoomEntity> filtered = allRooms.stream()
                .filter(r -> r.getRoomName() != null && r.getRoomName().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
            roomAdapter.submitList(filtered);
            tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void filterContacts(String query) {
        if (query == null || query.trim().isEmpty()) {
            contactAdapter.submitList(allContacts);
            tvEmpty.setVisibility(allContacts.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            List<UserEntity> filtered = allContacts.stream()
                .filter(c -> c.getFullName() != null && c.getFullName().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
            contactAdapter.submitList(filtered);
            tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void loadContacts() {
        progressLoading.setVisibility(View.VISIBLE);
        db.userDao().getAllUsers().observe(this, users -> {
            progressLoading.setVisibility(View.GONE);
            if (users != null) {
                List<UserEntity> activeUsers = users.stream()
                    .filter(u -> u.getUserId() != currentUserId && u.isActive())
                    .sorted((a, b) -> {
                        int rA = getRolePriority(a.getRole());
                        int rB = getRolePriority(b.getRole());
                        if (rA != rB) return Integer.compare(rA, rB);
                        return a.getFullName().compareTo(b.getFullName());
                    })
                    .collect(Collectors.toList());
                allContacts = activeUsers;
                if (!isRoomsTab) {
                    filterContacts(etSearch.getText().toString());
                }
            }
        });
    }

    private int getRolePriority(String role) {
        if (role == null) return 3;
        switch (role.toUpperCase()) {
            case "MANAGER": return 1;
            case "STAFF": return 2;
            case "ADMIN": return 3;
            default: return 4;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String token = SessionManager.getInstance(this).getToken();
        if (token != null) {
            viewModel.connectWebSocket(token);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        viewModel.disconnectWebSocket();
    }
}
