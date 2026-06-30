package com.example.cafe_manager.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.cafe_manager.data.local.entity.ShiftTemplateEntity;

import java.util.List;

@Dao
public interface ShiftTemplateDao {

    @Insert
    long insert(ShiftTemplateEntity template);

    @Update
    void update(ShiftTemplateEntity template);

    @Query("SELECT * FROM shift_templates ORDER BY start_time ASC")
    LiveData<List<ShiftTemplateEntity>> getAll();

    @Query("SELECT * FROM shift_templates WHERE is_active = 1 ORDER BY start_time ASC")
    LiveData<List<ShiftTemplateEntity>> getActive();

    @Query("SELECT * FROM shift_templates WHERE template_id = :id LIMIT 1")
    ShiftTemplateEntity getById(int id);

    @Query("UPDATE shift_templates SET is_active = 0 WHERE template_id = :id")
    void deactivate(int id);
}
