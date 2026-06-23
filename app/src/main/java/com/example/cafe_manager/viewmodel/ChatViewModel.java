package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.local.entity.ChatMessageEntity;
import com.example.cafe_manager.data.local.entity.ChatRoomEntity;
import com.example.cafe_manager.data.repository.ChatRepository;

import java.util.List;

public class ChatViewModel extends AndroidViewModel {
    private final ChatRepository repo;
    private final MutableLiveData<String> message = new MutableLiveData<>();

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
}
