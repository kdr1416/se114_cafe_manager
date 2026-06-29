package com.example.cafe_manager.data.websocket;

import android.util.Log;
import com.example.cafe_manager.BuildConfig;
import com.example.cafe_manager.data.remote.ChatMessageResponse;
import com.google.gson.Gson;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.util.HashMap;
import java.util.Map;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;

public class ChatWebSocketClient {
    private static final String TAG = "ChatWebSocketClient";
    private static volatile ChatWebSocketClient instance;

    private StompClient stompClient;
    private final Map<Integer, Disposable> roomSubscriptions;
    private final CompositeDisposable compositeDisposable;
    private boolean connected;
    private final Gson gson;

    private ChatWebSocketClient() {
        roomSubscriptions = new HashMap<>();
        compositeDisposable = new CompositeDisposable();
        gson = new Gson();
        connected = false;
    }

    public static ChatWebSocketClient getInstance() {
        if (instance == null) {
            synchronized (ChatWebSocketClient.class) {
                if (instance == null) {
                    instance = new ChatWebSocketClient();
                }
            }
        }
        return instance;
    }

    public void connect(String token) {
        if (connected) return;

        String wsUrl = BuildConfig.BASE_URL.replace("http", "ws") + "ws?token=" + token;
        Log.d(TAG, "Connecting to WebSocket at: " + wsUrl);

        try {
            stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl);

            Disposable connectionDisposable = stompClient.lifecycle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    lifecycleEvent -> {
                        switch (lifecycleEvent.getType()) {
                            case OPENED:
                                connected = true;
                                Log.d(TAG, "WebSocket connected");
                                break;
                            case CLOSED:
                                connected = false;
                                Log.d(TAG, "WebSocket closed");
                                cleanupSubscriptions();
                                break;
                            case ERROR:
                                connected = false;
                                Log.e(TAG, "WebSocket error", lifecycleEvent.getException());
                                cleanupSubscriptions();
                                break;
                        }
                    },
                    throwable -> Log.e(TAG, "Lifecycle error", throwable)
                );

            compositeDisposable.add(connectionDisposable);
            stompClient.connect();

        } catch (Exception e) {
            Log.e(TAG, "Failed to connect WebSocket", e);
        }
    }

    public void subscribeToRoom(int roomId, OnMessageListener listener) {
        if (stompClient == null) {
            listener.onError(new IllegalStateException("WebSocket not initialized"));
            return;
        }

        String topic = "/topic/chat/" + roomId;
        Log.d(TAG, "Subscribing to topic: " + topic);

        // Unsubscribe first if already subscribed
        unsubscribeFromRoom(roomId);

        Disposable subscription = stompClient.topic(topic)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                stompMessage -> {
                    try {
                        String json = stompMessage.getPayload();
                        Log.d(TAG, "Received WebSocket message: " + json);
                        ChatMessageResponse message = gson.fromJson(json, ChatMessageResponse.class);
                        listener.onMessage(message);
                    } catch (Exception e) {
                        listener.onError(e);
                    }
                },
                throwable -> listener.onError(throwable)
            );

        roomSubscriptions.put(roomId, subscription);
        compositeDisposable.add(subscription);
    }

    public void unsubscribeFromRoom(int roomId) {
        Disposable disposable = roomSubscriptions.remove(roomId);
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
            Log.d(TAG, "Unsubscribed from room: " + roomId);
        }
    }

    public void sendMessage(int roomId, String content) {
        if (!connected || stompClient == null) {
            Log.w(TAG, "Cannot send message, WebSocket not connected");
            return;
        }

        // The backend expectation: Send payload as json string
        // ChatMessagePayload in backend has content field
        Map<String, String> payload = new HashMap<>();
        payload.put("content", content);

        String destination = "/app/chat/" + roomId + "/send";
        String json = gson.toJson(payload);

        Log.d(TAG, "Sending message to destination: " + destination + " | Payload: " + json);
        stompClient.send(destination, json).subscribe(
            () -> Log.d(TAG, "Message sent successfully"),
            throwable -> Log.e(TAG, "Failed to send message", throwable)
        );
    }

    public void disconnect() {
        if (stompClient != null) {
            stompClient.disconnect();
        }
        compositeDisposable.clear();
        cleanupSubscriptions();
        connected = false;
        Log.d(TAG, "WebSocket disconnected");
    }

    public boolean isConnected() {
        return connected;
    }

    private void cleanupSubscriptions() {
        for (Disposable d : roomSubscriptions.values()) {
            if (d != null && !d.isDisposed()) {
                d.dispose();
            }
        }
        roomSubscriptions.clear();
    }

    public interface OnMessageListener {
        void onMessage(ChatMessageResponse message);
        void onError(Throwable throwable);
    }
}
