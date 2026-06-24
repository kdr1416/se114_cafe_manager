package com.example.cafe_manager.data.remote;

/**
 * Exception đại diện cho lỗi kết nối mạng (Network Error / No Internet).
 */
public class NetworkException extends Exception {
    public NetworkException(String message) {
        super(message);
    }

    public NetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
