package com.example.cafe_manager.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Query;

import com.example.cafe_manager.data.local.entity.ProductEntity;

import java.util.List;
@Dao
public interface ProductDao {
    @Query("SELECT * FROM products WHERE is_active = 1 ORDER BY product_name ASC")
    LiveData<List<ProductEntity>> getAllActive();

    @Query("SELECT * FROM products WHERE product_id = :categoryId AND is_active = 1 ORDER BY product_name ASC")
    LiveData<List<ProductEntity>> getByCategoryId(int categoryId);


    @Query("SELECT * FROM products WHERE product_id = :productId")
    ProductEntity getById(int productId);

    @Insert
    void insert(ProductEntity product);

    @Update
    void update(ProductEntity product);

    @Query("UPDATE products SET is_active = :isActive WHERE product_id = :productId")
    void setActive(int productId, boolean isActive);


}
