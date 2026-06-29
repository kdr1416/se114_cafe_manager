package com.example.cafe_manager.data.remote;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ChatApiService {
    @GET("api/v1/chat/rooms")
    Call<List<ChatRoomResponse>> getRooms();

    @GET("api/v1/chat/rooms/{roomId}/messages")
    Call<PageResponse<ChatMessageResponse>> getMessages(
        @Path("roomId") int roomId,
        @Query("page") int page,
        @Query("size") int size
    );

    @POST("api/v1/chat/rooms")
    Call<ChatRoomResponse> createRoom(@Body CreateRoomRequest request);

    @PUT("api/v1/chat/rooms/{roomId}/read")
    Call<Void> markRoomAsRead(@Path("roomId") int roomId);

    @GET("api/v1/chat/unread-count")
    Call<Integer> getUnreadCount();

    @POST("api/v1/chat/rooms/sync-shift/{shiftId}")
    Call<ChatRoomResponse> syncShiftRoom(@Path("shiftId") int shiftId);
}
