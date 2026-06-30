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
import com.example.cafe_manager.data.local.entity.ChatMessageEntity;
import com.example.cafe_manager.data.local.entity.ChatRoomEntity;
import com.example.cafe_manager.data.local.entity.UserEntity;
import com.example.cafe_manager.manager.SessionManager;
import com.example.cafe_manager.viewmodel.ChatViewModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatRoomListActivity extends AppCompatActivity {

    private ChatViewModel viewModel;
    private ChatRoomAdapter adapter;
    private AppDatabase db;
    private int currentUserId;


    private final Map<Integer, String> userNamesMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat_room_list);

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

        title.setText("Trò chuyện");
        caption.setText("Trò chuyện thảo luận công việc");
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        RecyclerView rvRooms = findViewById(R.id.rv_chat_rooms);
        rvRooms.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ChatRoomAdapter(currentUserId, room -> {
            Intent intent = new Intent(this, ChatMessageActivity.class);
            intent.putExtra("room_id", room.getRoomId());
            intent.putExtra("room_name", room.getRoomName());
            startActivity(intent);
        });
        rvRooms.setAdapter(adapter);

        // Setup empty state text
        View emptyState = findViewById(R.id.empty_state);
        TextView msg = emptyState.findViewById(R.id.tv_empty_message);
        msg.setText("Bạn chưa tham gia phòng chat nào.");
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        // Observe participant rooms
        viewModel.getRoomsByParticipant(currentUserId).observe(this, rooms -> {
            if (rooms != null) {
                adapter.submitList(rooms);

                // Observe unread counts and latest messages for each room dynamically
                for (ChatRoomEntity room : rooms) {
                    int roomId = room.getRoomId();
                    
                    // Subscribe to WebSocket updates for this room
                    viewModel.subscribeToRoom(roomId);

                    // Observe unread count
                    viewModel.getUnreadCountForRoom(roomId, currentUserId).observe(this, count -> {
                        adapter.setUnreadCount(roomId, count != null ? count : 0);
                    });

                    // Observe latest message
                    viewModel.getLatestMessage(roomId).observe(this, lastMsg -> {
                        if (lastMsg != null) {
                            adapter.setLatestMessage(roomId, lastMsg);
                        }
                    });
                }

                boolean empty = rooms.isEmpty();
                findViewById(R.id.rv_chat_rooms).setVisibility(empty ? View.GONE : View.VISIBLE);
                findViewById(R.id.empty_state).setVisibility(empty ? View.VISIBLE : View.GONE);
            }
        });

        // Observe all users to get full name mapping
        db.userDao().getAllUsers().observe(this, users -> {
            if (users != null) {
                userNamesMap.clear();
                for (UserEntity u : users) {
                    userNamesMap.put(u.getUserId(), u.getFullName());
                }
                adapter.setUserNames(userNamesMap);
            }
        });
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
