package com.example.cafe_manager.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.cafe_manager.data.local.entity.TableEntity;

import java.util.List;

@Dao
public interface TableDao {

    @Query("SELECT * FROM tables ORDER BY table_name ASC")
    LiveData<List<TableEntity>> getAll();

    @Query("SELECT * FROM tables WHERE table_id = :tableId")
    TableEntity getById(int tableId);

    @Insert
    void insert(TableEntity table);

    @Insert
    void insertAll(List<TableEntity> tables);

    @Query("UPDATE tables SET status = :status WHERE table_id = :tableId")
    void updateStatus(int tableId, String status);

    @Query("DELETE FROM tables")
    void deleteAll();
}