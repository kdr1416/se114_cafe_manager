package com.example.cafe_manager.util;

public interface RepositoryCallback<T> {
    void onSuccess(T result);
    void onError(Exception e);
}