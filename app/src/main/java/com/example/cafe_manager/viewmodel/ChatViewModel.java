package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.local.entity.ChatMessageEntity;
import com.example.cafe_manager.data.local.entity.ChatRoomEntity;
import com.example.cafe_manager.data.repository.ChatRepository;
import com.example.cafe_manager.util.RepositoryCallback;

import java.util.List;

public class ChatViewModel extends AndroidViewModel {
    private final ChatRepository repo;
    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isLoadingMore = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public ChatViewModel(@NonNull Application app) {
        super(app);
        repo = new ChatRepository(app);
    }

    public LiveData<List<ChatRoomEntity>> getActiveRooms() {
        return repo.getActiveRooms();
    }

    public LiveData<List<ChatRoomEntity>> getRoomsByParticipant(int userId) {
        return repo.getRoomsByParticipant(userId);
    }

    public LiveData<List<ChatMessageEntity>> getMessages(int roomId, int limit) {
        return repo.getMessages(roomId, limit);
    }

    public void loadMessages(int roomId) {
        isLoading.setValue(true);
        repo.loadMessages(roomId, 0, 50, new RepositoryCallback<>() {
            @Override
            public void onSuccess(List<ChatMessageEntity> result) {
                isLoading.setValue(false);
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                errorMessage.setValue(e.getMessage());
            }
        });
    }

    public void loadMoreMessages(int roomId) {
        if (Boolean.TRUE.equals(isLoadingMore.getValue())) return; // prevent duplicate calls
        isLoadingMore.setValue(true);
        int nextPage = repo.getCurrentPage(roomId) + 1;
        repo.loadMessages(roomId, nextPage, 50, new RepositoryCallback<>() {
            @Override
            public void onSuccess(List<ChatMessageEntity> result) {
                isLoadingMore.setValue(false);
            }

            @Override
            public void onError(Exception e) {
                isLoadingMore.setValue(false);
                // silently fail — don't show error for load more
            }
        });
    }

    public void switchRoom(int oldRoomId, int newRoomId) {
        if (oldRoomId != -1 && oldRoomId != newRoomId) {
            unsubscribeFromRoom(oldRoomId);
        }
        repo.resetPageForRoom(newRoomId);
        subscribeToRoom(newRoomId);
        loadMessages(newRoomId);
    }

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getIsLoadingMore() { return isLoadingMore; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public int getCurrentPage(int roomId) {
        return repo.getCurrentPage(roomId);
    }

    public LiveData<List<ChatMessageEntity>> getMessagesForRoom(int roomId) {
        return repo.getMessagesLiveData(roomId);
    }

    public void createRoom(String roomName, String roomType, Integer shiftId, String targetRole, int createdBy, Runnable onSuccess, Runnable onError) {
        ChatRoomEntity room = new ChatRoomEntity();
        room.setRoomName(roomName);
        room.setRoomType(roomType);
        room.setShiftId(shiftId);
        room.setTargetRole(targetRole);
        room.setCreatedBy(createdBy);
        room.setCreatedAt(System.currentTimeMillis());
        room.setUpdatedAt(System.currentTimeMillis());
        room.setIsActive(true);
        repo.createRoom(room, onSuccess, onError);
    }

    public void sendMessage(int roomId, int senderId, String content, Runnable onSuccess, Runnable onError) {
        if (content.trim().isEmpty()) {
            message.setValue("Nội dung không được rỗng");
            return;
        }
        repo.sendMessage(roomId, senderId, content.trim(), onSuccess, onError);
    }

    public void markMessageRead(int messageId, int userId) {
        repo.markMessageRead(messageId, userId);
    }

    public void markAllMessagesRead(int roomId, int userId) {
        repo.markAllMessagesRead(roomId, userId);
    }

    public void getUnreadCount(int roomId, int userId, com.example.cafe_manager.util.RepositoryCallback<Integer> callback) {
        repo.getUnreadCount(roomId, userId, callback);
    }

    public LiveData<ChatMessageEntity> getLatestMessage(int roomId) {
        return repo.getLatestMessage(roomId);
    }

    public LiveData<Integer> getUnreadCountForRoom(int roomId, int userId) {
        return repo.getUnreadCountLive(roomId, userId);
    }

    public LiveData<String> getMessage() {
        return message;
    }

    public void clearMessage() {
        message.setValue(null);
    }

    public void connectWebSocket(String token) {
        repo.connectWebSocket(token);
    }

    public void disconnectWebSocket() {
        repo.disconnectWebSocket();
    }

    public void subscribeToRoom(int roomId) {
        repo.subscribeToRoom(roomId);
    }

    public void unsubscribeFromRoom(int roomId) {
        repo.unsubscribeFromRoom(roomId);
    }
}
