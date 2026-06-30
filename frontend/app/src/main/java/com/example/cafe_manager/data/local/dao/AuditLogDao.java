package com.example.cafe_manager.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.cafe_manager.data.local.entity.AuditLogEntity;

import java.util.List;

@Dao
public interface AuditLogDao {

    @Insert
    long insert(AuditLogEntity log);

    @Query("SELECT * FROM audit_logs ORDER BY created_at DESC")
    LiveData<List<AuditLogEntity>> getAll();

    @Query("SELECT * FROM audit_logs WHERE user_id = :userId ORDER BY created_at DESC")
    LiveData<List<AuditLogEntity>> getByUserId(int userId);
}
