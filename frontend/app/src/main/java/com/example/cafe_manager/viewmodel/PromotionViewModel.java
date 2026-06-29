package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.local.entity.PromotionEntity;
import com.example.cafe_manager.data.repository.PromotionRepository;

import java.util.List;

public class PromotionViewModel extends AndroidViewModel {

    private final PromotionRepository repository;
    private final LiveData<List<PromotionEntity>> promotions;
    private final LiveData<Long> totalCount;
    private final LiveData<Long> activeCount;
    private final MutableLiveData<String> message = new MutableLiveData<>();

    public PromotionViewModel(@NonNull Application application) {
        super(application);
        repository = PromotionRepository.getInstance(application);
        promotions = repository.getAll();
        totalCount = repository.getTotalCountLive();
        activeCount = repository.getActiveCountLive();
    }

    public LiveData<List<PromotionEntity>> getPromotions() { return promotions; }
    public LiveData<Long> getTotalCount() { return totalCount; }
    public LiveData<Long> getActiveCount() { return activeCount; }
    public LiveData<String> getMessage() { return message; }

    public void clearMessage() { message.setValue(null); }

    public void addPromotion(String code, String type, double value, long expiresAt) {
        PromotionEntity p = new PromotionEntity();
        p.setCode(code.trim().toUpperCase());
        p.setType(type);
        p.setValue(value);
        p.setActive(true);
        p.setExpiresAt(expiresAt);
        p.setCreatedAt(System.currentTimeMillis());

        repository.insert(p,
                () -> message.setValue("Thêm mã giảm giá thành công"),
                () -> message.setValue("Mã giảm giá đã tồn tại"));
    }

    public void updatePromotion(PromotionEntity promotion) {
        repository.update(promotion,
                () -> message.setValue("Cập nhật thành công"));
    }

    public void toggleActive(int id, boolean active) {
        repository.toggleActive(id, active);
    }

    public void delete(int id) {
        repository.delete(id);
        message.setValue("Đã xoá mã giảm giá");
    }
}
