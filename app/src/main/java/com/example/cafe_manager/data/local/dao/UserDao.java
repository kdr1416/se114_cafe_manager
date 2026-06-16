package com.example.cafe_manager.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Query;

import com.example.cafe_manager.data.local.entity.UserEntity;

import java.util.List;

@Dao
public interface UserDao {

    @Insert
    long insert(UserEntity user);

    @Update
    void update(UserEntity user);

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    UserEntity getByUsername(String username);

    @Query("SELECT * FROM users WHERE user_id = :userId LIMIT 1")
    UserEntity getById(int userId);

    @Query("SELECT * FROM users ORDER BY role ASC, full_name ASC")
    LiveData<List<UserEntity>> getAllUsers();

    @Query("SELECT * FROM users WHERE role = :role ORDER BY full_name ASC")
    LiveData<List<UserEntity>> getUsersByRole(String role);

    @Query("UPDATE users SET is_active = :isActive WHERE user_id = :userId")
    void setActive(int userId, boolean isActive);

    @Query("UPDATE users SET password_hash = :passwordHash WHERE user_id = :userId")
    void updatePassword(int userId, String passwordHash);

    @Query("UPDATE users SET role = :role WHERE user_id = :userId")
    void updateRole(int userId, String role);

    @Query("UPDATE users SET last_login_at = :time WHERE user_id = :userId")
    void updateLastLogin(int userId, long time);
}
