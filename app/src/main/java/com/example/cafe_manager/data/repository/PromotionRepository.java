package com.example.cafe_manager.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.dao.PromotionDao;
import com.example.cafe_manager.data.local.entity.PromotionEntity;
import com.example.cafe_manager.util.AppExecutors;

import java.util.List;

public class PromotionRepository {

    private final PromotionDao promotionDao;
    private final AppExecutors appExecutors;

    public PromotionRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.promotionDao = db.promotionDao();
        this.appExecutors = AppExecutors.getInstance();
    }

    public LiveData<List<PromotionEntity>> getAll() {
        return promotionDao.getAll();
    }

    public LiveData<Integer> getTotalCount() {
        return promotionDao.getTotalCount();
    }

    public LiveData<Integer> getActiveCount() {
        return promotionDao.getActiveCount();
    }

    public PromotionEntity getByCode(String code) {
        if (code == null) return null;
        return promotionDao.getByCode(code.trim().toUpperCase());
    }

    public void insert(PromotionEntity promotion, Runnable onSuccess, Runnable onError) {
        appExecutors.diskIO().execute(() -> {
            try {
                PromotionEntity existing = promotionDao.getByCode(promotion.getCode());
                if (existing != null) {
                    appExecutors.mainThread().execute(onError);
                    return;
                }
                promotionDao.insert(promotion);
                appExecutors.mainThread().execute(onSuccess);
            } catch (Exception e) {
                appExecutors.mainThread().execute(onError);
            }
        });
    }

    public void update(PromotionEntity promotion, Runnable onSuccess) {
        appExecutors.diskIO().execute(() -> {
            promotionDao.update(promotion);
            appExecutors.mainThread().execute(onSuccess);
        });
    }

    public void toggleActive(int id, boolean active) {
        appExecutors.diskIO().execute(() -> promotionDao.updateActive(id, active));
    }

    public void delete(int id) {
        appExecutors.diskIO().execute(() -> promotionDao.deleteById(id));
    }
}
