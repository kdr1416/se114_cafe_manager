package com.example.cafe_manager.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.entity.ChatMessageEntity;
import com.example.cafe_manager.data.local.entity.ChatRoomEntity;
import com.example.cafe_manager.data.remote.ApiClient;
import com.example.cafe_manager.data.remote.ChatApiService;
import com.example.cafe_manager.data.remote.ChatMessageResponse;
import com.example.cafe_manager.data.remote.ChatRoomResponse;
import com.example.cafe_manager.data.remote.CreateRoomRequest;
import com.example.cafe_manager.data.remote.PageResponse;
import com.example.cafe_manager.data.websocket.ChatWebSocketClient;
import com.example.cafe_manager.util.AppExecutors;

import com.example.cafe_manager.util.RepositoryCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatRepository {
    private static final String TAG = "ChatRepository";
    private static volatile ChatRepository instance;

    private final ChatApiService apiService;
    private final ChatWebSocketClient webSocketClient;
    private final AppExecutors exec;
    private final Context appContext;

    // Shared static caches to maintain consistency across all instances
    private static final MutableLiveData<List<ChatRoomEntity>> activeRoomsCache = new MutableLiveData<>(new ArrayList<>());
    private static final Map<Integer, MutableLiveData<List<ChatMessageEntity>>> messageCacheByRoom = new HashMap<>();
    private static final Map<Integer, MutableLiveData<Integer>> unreadCountCache = new HashMap<>();
    private static final Map<Integer, MutableLiveData<ChatMessageEntity>> latestMessageCache = new HashMap<>();
    private final Map<Integer, Integer> currentPageByRoom = new ConcurrentHashMap<>();

    private ChatWebSocketClient.OnMessageListener wsMessageListener;

    public ChatRepository(Context ctx) {
        this.appContext = ctx.getApplicationContext();
        this.apiService = ApiClient.getInstance(appContext).getService(ChatApiService.class);
        this.webSocketClient = ChatWebSocketClient.getInstance();
        this.exec = AppExecutors.getInstance();
        setupWebSocketListener();
        
        // Save the first initialized instance as the singleton
        if (instance == null) {
            synchronized (ChatRepository.class) {
                if (instance == null) {
                    instance = this;
                }
            }
        }
    }

    public static ChatRepository getInstance(Context ctx) {
        if (instance == null) {
            synchronized (ChatRepository.class) {
                if (instance == null) {
                    instance = new ChatRepository(ctx);
                }
            }
        }
        return instance;
    }

    private void setupWebSocketListener() {
        wsMessageListener = new ChatWebSocketClient.OnMessageListener() {
            @Override
            public void onMessage(ChatMessageResponse response) {
                exec.mainThread().execute(() -> handleWebSocketMessage(response));
            }

            @Override
            public void onError(Throwable throwable) {
                Log.e(TAG, "WebSocket message error", throwable);
            }
        };
    }

    private MutableLiveData<List<ChatMessageEntity>> getOrCreateMessageLiveData(int roomId) {
        synchronized (messageCacheByRoom) {
            return messageCacheByRoom.computeIfAbsent(roomId, k -> new MutableLiveData<>(new ArrayList<>()));
        }
    }

    public void resetPageForRoom(int roomId) {
        currentPageByRoom.put(roomId, 0);
    }

    public int getCurrentPage(int roomId) {
        return currentPageByRoom.getOrDefault(roomId, 0);
    }

    public LiveData<List<ChatMessageEntity>> getMessagesLiveData(int roomId) {
        return getOrCreateMessageLiveData(roomId);
    }

    private void handleWebSocketMessage(ChatMessageResponse remoteMsg) {
        int roomId = remoteMsg.getRoomId();
        int senderId = remoteMsg.getSenderId();

        ChatMessageEntity localMsg = new ChatMessageEntity();
        localMsg.setMessageId(remoteMsg.getMessageId());
        localMsg.setRoomId(roomId);
        localMsg.setSenderId(senderId);
        localMsg.setSenderName(remoteMsg.getSenderName());
        localMsg.setContent(remoteMsg.getContent());
        localMsg.setCreatedAt(remoteMsg.getCreatedAt());
        localMsg.setIsDeleted(remoteMsg.getIsDeleted() != null ? remoteMsg.getIsDeleted() : false);

        // Update message cache
        MutableLiveData<List<ChatMessageEntity>> messagesLive = getOrCreateMessageLiveData(roomId);
        List<ChatMessageEntity> current = messagesLive.getValue();
        if (current == null) current = new ArrayList<>();
        List<ChatMessageEntity> updated = new ArrayList<>();
        
        // Prevent duplicate messages, and remove corresponding optimistic/temporary messages
        boolean exists = false;
        for (ChatMessageEntity msg : current) {
            if (msg.getMessageId() <= 0) {
                // If it is our optimistic message, remove it since we are adding the real one
                if (msg.getSenderId() == senderId && msg.getContent().equals(localMsg.getContent())) {
                    continue;
                }
                // Cleanup stale optimistic messages older than 10 seconds
                if (System.currentTimeMillis() - msg.getCreatedAt() > 10000) {
                    continue;
                }
                updated.add(msg);
            } else {
                if (msg.getMessageId() == localMsg.getMessageId()) {
                    exists = true;
                }
                updated.add(msg);
            }
        }
        
        if (!exists) {
            updated.add(localMsg);
            updated.sort((a, b) -> Long.compare(a.getCreatedAt(), b.getCreatedAt()));
            messagesLive.setValue(updated);
        }

        // Update latest message cache
        MutableLiveData<ChatMessageEntity> latestLive = latestMessageCache.get(roomId);
        if (latestLive == null) {
            latestLive = new MutableLiveData<>();
            latestMessageCache.put(roomId, latestLive);
        }
        latestLive.setValue(localMsg);

        // Update unread count if not sent by self
        int currentUserId = com.example.cafe_manager.manager.SessionManager.getInstance(appContext).getUserId();
        if (senderId != currentUserId) {
            MutableLiveData<Integer> unreadLive = unreadCountCache.get(roomId);
            if (unreadLive == null) {
                unreadLive = new MutableLiveData<>(0);
                unreadCountCache.put(roomId, unreadLive);
            }
            Integer currentCount = unreadLive.getValue();
            unreadLive.setValue(currentCount != null ? currentCount + 1 : 1);
        }

        // Refresh rooms list from API to get updated metadata (e.g. unread count or sorting)
        refreshRoomsFromApi(null);
    }

    // ── Room operations ──

    public LiveData<List<ChatRoomEntity>> getActiveRooms() {
        refreshRoomsFromApi(null);
        return activeRoomsCache;
    }

    public LiveData<List<ChatRoomEntity>> getRoomsByParticipant(int userId) {
        refreshRoomsFromApi(null);
        return activeRoomsCache;
    }

    public void createRoom(ChatRoomEntity room, Runnable onSuccess, Runnable onError) {
        CreateRoomRequest request = new CreateRoomRequest(room.getRoomName(), room.getTargetRole());
        apiService.createRoom(request).enqueue(new Callback<ChatRoomResponse>() {
            @Override
            public void onResponse(Call<ChatRoomResponse> call, Response<ChatRoomResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    refreshRoomsFromApi(onSuccess);
                } else {
                    if (onError != null) exec.mainThread().execute(onError);
                }
            }

            @Override
            public void onFailure(Call<ChatRoomResponse> call, Throwable t) {
                if (onError != null) exec.mainThread().execute(onError);
            }
        });
    }

    // ── Message operations ──

    public LiveData<List<ChatMessageEntity>> getMessages(int roomId, int limit) {
        loadMessages(roomId, 0, limit, null);
        return getOrCreateMessageLiveData(roomId);
    }

    private List<ChatMessageEntity> mapToEntities(List<ChatMessageResponse> list) {
        List<ChatMessageEntity> entities = new ArrayList<>();
        if (list != null) {
            for (ChatMessageResponse r : list) {
                ChatMessageEntity e = new ChatMessageEntity();
                e.setMessageId(r.getMessageId());
                e.setRoomId(r.getRoomId());
                e.setSenderId(r.getSenderId());
                e.setSenderName(r.getSenderName());
                e.setContent(r.getContent());
                e.setCreatedAt(r.getCreatedAt());
                e.setIsDeleted(r.getIsDeleted() != null ? r.getIsDeleted() : false);
                entities.add(e);
            }
        }
        entities.sort((a, b) -> Long.compare(a.getCreatedAt(), b.getCreatedAt()));
        return entities;
    }

    public void loadMessages(int roomId, int page, int size, RepositoryCallback<List<ChatMessageEntity>> callback) {
        exec.diskIO().execute(() -> {
            try {
                Response<PageResponse<ChatMessageResponse>> response =
                    apiService.getMessages(roomId, page, size).execute();
                if (response.isSuccessful() && response.body() != null) {
                    List<ChatMessageEntity> newMessages = mapToEntities(response.body().getContent());

                    if (page == 0) {
                        // Initial load — replace entire list
                        exec.mainThread().execute(() -> {
                            getOrCreateMessageLiveData(roomId).setValue(newMessages);
                            currentPageByRoom.put(roomId, 0);
                            if (callback != null) callback.onSuccess(newMessages);
                        });
                    } else {
                        // Load more — PREPEND old messages to front of existing list
                        exec.mainThread().execute(() -> {
                            MutableLiveData<List<ChatMessageEntity>> liveData = getOrCreateMessageLiveData(roomId);
                            List<ChatMessageEntity> current = liveData.getValue();
                            List<ChatMessageEntity> merged = new ArrayList<>();
                            merged.addAll(newMessages);  // older messages first
                            if (current != null) {
                                // Deduplicate: only add current items not already in newMessages
                                Set<Integer> newIds = newMessages.stream()
                                    .map(ChatMessageEntity::getMessageId)
                                    .collect(Collectors.toSet());
                                for (ChatMessageEntity m : current) {
                                    if (!newIds.contains(m.getMessageId())) {
                                        merged.add(m);
                                    }
                                }
                            }
                            liveData.setValue(merged);
                            currentPageByRoom.put(roomId, page);
                            if (callback != null) callback.onSuccess(merged);
                        });
                    }
                } else {
                    exec.mainThread().execute(() -> {
                        if (callback != null) callback.onError(new Exception("Load failed: " + response.code()));
                    });
                }
            } catch (Exception e) {
                exec.mainThread().execute(() -> {
                    if (callback != null) callback.onError(e);
                });
            }
        });
    }

    public void sendMessage(int roomId, int senderId, String content, Runnable onSuccess, Runnable onError) {
        if (webSocketClient.isConnected()) {
            // Create optimistic local message
            ChatMessageEntity optimisticMsg = new ChatMessageEntity();
            // Assign a temporary unique negative ID
            optimisticMsg.setMessageId(-1 - (int)(System.currentTimeMillis() % 10000));
            optimisticMsg.setRoomId(roomId);
            optimisticMsg.setSenderId(senderId);
            String currentFullName = com.example.cafe_manager.manager.SessionManager.getInstance(appContext).getFullName();
            optimisticMsg.setSenderName(currentFullName != null && !currentFullName.isEmpty() ? currentFullName : "Bạn");
            optimisticMsg.setContent(content);
            optimisticMsg.setCreatedAt(System.currentTimeMillis());
            optimisticMsg.setIsDeleted(false);

            // Insert optimistic message immediately
            MutableLiveData<List<ChatMessageEntity>> messagesLive = getOrCreateMessageLiveData(roomId);
            List<ChatMessageEntity> current = messagesLive.getValue();
            if (current == null) current = new ArrayList<>();
            List<ChatMessageEntity> updated = new ArrayList<>(current);
            updated.add(optimisticMsg);
            updated.sort((a, b) -> Long.compare(a.getCreatedAt(), b.getCreatedAt()));
            messagesLive.setValue(updated);

            // Also update latest message cache immediately
            MutableLiveData<ChatMessageEntity> latestLive = latestMessageCache.get(roomId);
            if (latestLive == null) {
                latestLive = new MutableLiveData<>();
                latestMessageCache.put(roomId, latestLive);
            }
            latestLive.setValue(optimisticMsg);

            // Send via WebSocket
            webSocketClient.sendMessage(roomId, content);
            if (onSuccess != null) {
                exec.mainThread().execute(onSuccess);
            }
        } else {
            if (onError != null) {
                exec.mainThread().execute(onError);
            }
        }
    }

    // ── Read tracking ──

    public void markMessageRead(int messageId, int userId) {
        // Backend only supports room-wide read tracking
    }

    public void markAllMessagesRead(int roomId, int userId) {
        apiService.markRoomAsRead(roomId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    MutableLiveData<Integer> unreadLive = unreadCountCache.get(roomId);
                    if (unreadLive != null) {
                        unreadLive.setValue(0);
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Failed to mark room as read: " + roomId, t);
            }
        });
    }

    public void getUnreadCount(int roomId, int userId, com.example.cafe_manager.util.RepositoryCallback<Integer> callback) {
        apiService.getRooms().enqueue(new Callback<List<ChatRoomResponse>>() {
            @Override
            public void onResponse(Call<List<ChatRoomResponse>> call, Response<List<ChatRoomResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (ChatRoomResponse r : response.body()) {
                        if (r.getRoomId() == roomId) {
                            callback.onSuccess(r.getUnreadCount());
                            return;
                        }
                    }
                    callback.onSuccess(0);
                } else {
                    callback.onError(new Exception("Failed to get unread count"));
                }
            }

            @Override
            public void onFailure(Call<List<ChatRoomResponse>> call, Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }

    public LiveData<ChatMessageEntity> getLatestMessage(int roomId) {
        MutableLiveData<ChatMessageEntity> liveData;
        synchronized (latestMessageCache) {
            liveData = latestMessageCache.get(roomId);
            if (liveData == null) {
                liveData = new MutableLiveData<>();
                latestMessageCache.put(roomId, liveData);
            }
        }
        return liveData;
    }

    public LiveData<Integer> getUnreadCountLive(int roomId, int userId) {
        MutableLiveData<Integer> liveData;
        synchronized (unreadCountCache) {
            liveData = unreadCountCache.get(roomId);
            if (liveData == null) {
                liveData = new MutableLiveData<>(0);
                unreadCountCache.put(roomId, liveData);
            }
        }
        return liveData;
    }

    // ── Static method for backward compatibility ──

    public static void syncShiftChatRoomSync(AppDatabase db, int shiftId) {
        try {
            if (instance != null) {
                instance.apiService.syncShiftRoom(shiftId).execute();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── New instance method for sync shift room ──

    public void syncShiftRoom(int shiftId, Runnable onSuccess, Runnable onError) {
        apiService.syncShiftRoom(shiftId).enqueue(new Callback<ChatRoomResponse>() {
            @Override
            public void onResponse(Call<ChatRoomResponse> call, Response<ChatRoomResponse> response) {
                if (response.isSuccessful()) {
                    refreshRoomsFromApi(onSuccess);
                } else {
                    if (onError != null) exec.mainThread().execute(onError);
                }
            }

            @Override
            public void onFailure(Call<ChatRoomResponse> call, Throwable t) {
                if (onError != null) exec.mainThread().execute(onError);
            }
        });
    }

    // ── Refresh helpers ──

    public void refreshRoomsFromApi(Runnable onComplete) {
        apiService.getRooms().enqueue(new Callback<List<ChatRoomResponse>>() {
            @Override
            public void onResponse(Call<List<ChatRoomResponse>> call, Response<List<ChatRoomResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ChatRoomEntity> entities = new ArrayList<>();
                    for (ChatRoomResponse r : response.body()) {
                        ChatRoomEntity e = new ChatRoomEntity();
                        e.setRoomId(r.getRoomId());
                        e.setRoomName(r.getRoomName());
                        e.setRoomType(r.getRoomType());
                        e.setShiftId(r.getShiftId());
                        e.setIsActive(r.getIsActive() != null ? r.getIsActive() : true);
                        entities.add(e);

                        // Update latest message cache if present
                        if (r.getLastMessage() != null) {
                            MutableLiveData<ChatMessageEntity> latestLive = latestMessageCache.get(r.getRoomId());
                            if (latestLive == null) {
                                latestLive = new MutableLiveData<>();
                                latestMessageCache.put(r.getRoomId(), latestLive);
                            }
                            ChatMessageEntity localMsg = new ChatMessageEntity();
                            localMsg.setRoomId(r.getRoomId());
                            localMsg.setContent(r.getLastMessage());
                            localMsg.setCreatedAt(r.getLastMessageAt() != null ? r.getLastMessageAt() : 0);
                            latestLive.setValue(localMsg);
                        }

                        // Update unread count cache
                        if (r.getUnreadCount() != null) {
                            MutableLiveData<Integer> unreadLive = unreadCountCache.get(r.getRoomId());
                            if (unreadLive == null) {
                                unreadLive = new MutableLiveData<>(0);
                                unreadCountCache.put(r.getRoomId(), unreadLive);
                            }
                            unreadLive.setValue(r.getUnreadCount());
                        }
                    }
                    activeRoomsCache.setValue(entities);
                }
                if (onComplete != null) {
                    onComplete.run();
                }
            }

            @Override
            public void onFailure(Call<List<ChatRoomResponse>> call, Throwable t) {
                Log.e(TAG, "Failed to refresh rooms", t);
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }

    // ── WebSocket connection helpers ──

    public void connectWebSocket(String token) {
        webSocketClient.connect(token);
    }

    public void disconnectWebSocket() {
        webSocketClient.disconnect();
    }

    public void subscribeToRoom(int roomId) {
        webSocketClient.subscribeToRoom(roomId, wsMessageListener);
    }

    public void unsubscribeFromRoom(int roomId) {
        webSocketClient.unsubscribeFromRoom(roomId);
    }
}
