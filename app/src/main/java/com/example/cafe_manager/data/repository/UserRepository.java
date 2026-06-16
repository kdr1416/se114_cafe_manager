package com.example.cafe_manager.data.repository;

import android.content.Context;

import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.dao.UserDao;
import com.example.cafe_manager.data.local.entity.UserEntity;

public class UserRepository {

    private final UserDao userDao;

    public UserRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.userDao = db.userDao();
    }

    /** Sync — caller phải gọi trên background thread. */
    public UserEntity getByUsername(String username) {
        if (username == null) return null;
        return userDao.getByUsername(username.trim());
    }
}
