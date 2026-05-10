package com.example.cafe_manager.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Query;

import com.example.cafe_manager.data.local.entity.CategoryEntity;

import java.util.List;

@Dao
public interface CategoryDao {

    @Query("SELECT * FROM categories WHERE is_active = 1 ORDER BY category_name ASC")
    LiveData<List<CategoryEntity>> getAllActive();

    @Query("SELECT * FROM categories ORDER BY category_name ASC")
    LiveData<List<CategoryEntity>> getAll();

    @Query("SELECT * FROM categories WHERE category_id = :categoryId")
    CategoryEntity getById(int categoryId);

    @Insert
    long insert(CategoryEntity category);

    @Update
    void update(CategoryEntity category);

    @Query("UPDATE categories SET is_active = :isActive WHERE category_id = :categoryId")
    void setActive(int categoryId, boolean isActive);
}