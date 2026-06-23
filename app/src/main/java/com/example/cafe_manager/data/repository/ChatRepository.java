package com.example.cafe_manager.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.dao.ChatMessageDao;
import com.example.cafe_manager.data.local.dao.ChatParticipantDao;
import com.example.cafe_manager.data.local.dao.ChatRoomDao;
import com.example.cafe_manager.data.local.dao.ChatReadDao;
import com.example.cafe_manager.data.local.entity.ChatMessageEntity;
import com.example.cafe_manager.data.local.entity.ChatParticipantEntity;
import com.example.cafe_manager.data.local.entity.ChatRoomEntity;
import com.example.cafe_manager.data.local.entity.ShiftEntity;
import com.example.cafe_manager.data.local.entity.ShiftAssignmentEntity;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.Constants;

import java.util.List;

public class ChatRepository {
    private final ChatRoomDao roomDao;
    private final ChatMessageDao messageDao;
    private final ChatReadDao readDao;
    private final ChatParticipantDao participantDao;
    private final AppExecutors exec;

    public ChatRepository(Context ctx) {
        AppDatabase db = AppDatabase.getInstance(ctx);
        this.roomDao = db.chatRoomDao();
        this.messageDao = db.chatMessageDao();
        this.readDao = db.chatReadDao();
        this.participantDao = db.chatParticipantDao();
        this.exec = AppExecutors.getInstance();
    }

    // Room operations
    public LiveData<List<ChatRoomEntity>> getActiveRooms() {
        return roomDao.getAllActive();
    }

    public LiveData<List<ChatRoomEntity>> getRoomsByParticipant(int userId) {
        return roomDao.getByParticipant(userId);
    }

    public void createRoom(ChatRoomEntity room, Runnable onSuccess, Runnable onError) {
        exec.diskIO().execute(() -> {
            try {
                long roomId = roomDao.insert(room);
                // Auto-add creator as participant
                ChatParticipantEntity p = new ChatParticipantEntity();
                p.setRoomId((int) roomId);
                p.setUserId(room.getCreatedBy());
                p.setJoinedAt(System.currentTimeMillis());
                p.setRoleInRoom("OWNER");
                participantDao.insert(p);
                exec.mainThread().execute(onSuccess);
            } catch (Exception e) {
                exec.mainThread().execute(onError);
            }
        });
    }

    // Message operations
    public LiveData<List<ChatMessageEntity>> getMessages(int roomId, int limit) {
        return messageDao.getByRoom(roomId, limit);
    }

    public void sendMessage(int roomId, int senderId, String content, Runnable onSuccess, Runnable onError) {
        exec.diskIO().execute(() -> {
            try {
                ChatMessageEntity msg = new ChatMessageEntity();
                msg.setRoomId(roomId);
                msg.setSenderId(senderId);
                msg.setContent(content);
                msg.setCreatedAt(System.currentTimeMillis());
                msg.setIsDeleted(false);
                messageDao.insert(msg);

                // Update room's updated_at timestamp
                ChatRoomEntity room = roomDao.getById(roomId);
                if (room != null) {
                    room.setUpdatedAt(System.currentTimeMillis());
                    roomDao.update(room);
                }
                exec.mainThread().execute(onSuccess);
            } catch (Exception e) {
                if (onError != null) {
                    exec.mainThread().execute(onError);
                } else {
                    e.printStackTrace();
                }
            }
        });
    }

    // Read tracking
    public void markMessageRead(int messageId, int userId) {
        exec.diskIO().execute(() -> readDao.markMessageReadIfNeeded(messageId, userId, System.currentTimeMillis()));
    }

    public void markAllMessagesRead(int roomId, int userId) {
        exec.diskIO().execute(() -> readDao.markAllMessagesRead(roomId, userId, System.currentTimeMillis()));
    }

    public void getUnreadCount(int roomId, int userId, com.example.cafe_manager.util.RepositoryCallback<Integer> callback) {
        exec.diskIO().execute(() -> {
            int count = messageDao.getUnreadCountSync(roomId, userId);
            exec.mainThread().execute(() -> callback.onSuccess(count));
        });
    }

    public LiveData<ChatMessageEntity> getLatestMessage(int roomId) {
        return messageDao.getLatest(roomId);
    }

    public LiveData<Integer> getUnreadCountLive(int roomId, int userId) {
        return messageDao.getUnreadCountLive(roomId, userId);
    }

    public static void syncShiftChatRoomSync(AppDatabase db, int shiftId) {
        db.runInTransaction(() -> {
            try {
                ShiftEntity shift = db.shiftDao().getById(shiftId);
                if (shift == null) return;

                ChatRoomEntity room = db.chatRoomDao().getByShiftId(shiftId);
                int roomId;
                if (room == null) {
                    if (Constants.SHIFT_CANCELLED.equals(shift.getStatus())) {
                        return; // Don't create chat room for cancelled shifts
                    }
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
                    String dateStr = sdf.format(new java.util.Date(shift.getShiftDate()));
                    String roomName = shift.getShiftName() + " - " + dateStr + " (" + shift.getStartTime() + "-" + shift.getEndTime() + ")";

                    room = new ChatRoomEntity();
                    room.setRoomName(roomName);
                    room.setRoomType(Constants.CHAT_TYPE_SHIFT);
                    room.setShiftId(shiftId);
                    room.setCreatedBy(shift.getOpenedBy() > 0 ? shift.getOpenedBy() : 1);
                    room.setCreatedAt(System.currentTimeMillis());
                    room.setUpdatedAt(System.currentTimeMillis());
                    room.setIsActive(true);
                    roomId = (int) db.chatRoomDao().insert(room);
                } else {
                    if (Constants.SHIFT_CANCELLED.equals(shift.getStatus())) {
                        room.setIsActive(false);
                        db.chatRoomDao().update(room);
                        return;
                    }

                    // Update name if shift info changed
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
                    String dateStr = sdf.format(new java.util.Date(shift.getShiftDate()));
                    String expectedName = shift.getShiftName() + " - " + dateStr + " (" + shift.getStartTime() + "-" + shift.getEndTime() + ")";

                    boolean changed = false;
                    if (!expectedName.equals(room.getRoomName())) {
                        room.setRoomName(expectedName);
                        changed = true;
                    }
                    if (!room.getIsActive()) {
                        room.setIsActive(true);
                        changed = true;
                    }
                    if (changed) {
                        db.chatRoomDao().update(room);
                    }
                    roomId = room.getRoomId();
                }

                // Sync participants
                List<ShiftAssignmentEntity> assignments = db.shiftAssignmentDao().getByShiftSync(shiftId);
                List<ChatParticipantEntity> currentParticipants = db.chatParticipantDao().getByRoom(roomId);

                // Add missing
                for (ShiftAssignmentEntity assign : assignments) {
                    boolean exists = false;
                    for (ChatParticipantEntity part : currentParticipants) {
                        if (part.getUserId() == assign.getUserId()) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        ChatParticipantEntity p = new ChatParticipantEntity();
                        p.setRoomId(roomId);
                        p.setUserId(assign.getUserId());
                        p.setJoinedAt(System.currentTimeMillis());
                        p.setRoleInRoom(Constants.CHAT_ROLE_MEMBER);
                        db.chatParticipantDao().insert(p);
                    }
                }

                // Remove extra
                for (ChatParticipantEntity part : currentParticipants) {
                    boolean stillAssigned = false;
                    for (ShiftAssignmentEntity assign : assignments) {
                        if (assign.getUserId() == part.getUserId()) {
                            stillAssigned = true;
                            break;
                        }
                    }
                    if (!stillAssigned) {
                        db.chatParticipantDao().deleteParticipant(roomId, part.getUserId());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
