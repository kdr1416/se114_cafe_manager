package com.example.cafe_manager.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.cafe_manager.data.local.entity.AreaEntity;

import java.util.List;

@Dao
public interface AreaDao {

    @Query("SELECT * FROM areas ORDER BY area_name ASC")
    LiveData<List<AreaEntity>> getAll();

    @Query("SELECT * FROM areas WHERE area_id = :areaId")
    AreaEntity getById(int areaId);

    @Query("SELECT * FROM areas WHERE area_name = :areaName LIMIT 1")
    AreaEntity getByName(String areaName);

    @Insert
    void insert(AreaEntity area);

    @Insert
    void insertAll(List<AreaEntity> areas);

    @Update
    void update(AreaEntity area);

    @Query("DELETE FROM areas WHERE area_id = :areaId")
    void deleteById(int areaId);

    @Query("SELECT COUNT(*) FROM tables WHERE area = :areaName")
    int countTablesByArea(String areaName);

    @Query("DELETE FROM areas")
    void deleteAll();
}
