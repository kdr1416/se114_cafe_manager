package com.example.cafe_manager.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.cafe_manager.data.local.entity.PromotionEntity;

import java.util.List;

@Dao
public interface PromotionDao {

    @Insert
    long insert(PromotionEntity promotion);

    @Update
    void update(PromotionEntity promotion);

    @Query("SELECT * FROM promotions ORDER BY created_at DESC")
    LiveData<List<PromotionEntity>> getAll();

    @Query("SELECT * FROM promotions WHERE code = :code LIMIT 1")
    PromotionEntity getByCode(String code);

    @Query("UPDATE promotions SET is_active = :active WHERE promotion_id = :id")
    void updateActive(int id, boolean active);

    @Query("DELETE FROM promotions WHERE promotion_id = :id")
    void deleteById(int id);

    @Query("SELECT COUNT(*) FROM promotions")
    LiveData<Integer> getTotalCount();

    @Query("SELECT COUNT(*) FROM promotions WHERE is_active = 1")
    LiveData<Integer> getActiveCount();
}
