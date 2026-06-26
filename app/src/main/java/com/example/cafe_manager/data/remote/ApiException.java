package com.example.cafe_manager.data.remote;

public class ApiException extends Exception {
    private final int httpCode;

    public ApiException(int httpCode, String message) {
        super(message);
        this.httpCode = httpCode;
    }

    public int getHttpCode() {
        return httpCode;
    }
}
