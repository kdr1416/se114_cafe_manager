package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.local.entity.ShiftTemplateEntity;
import com.example.cafe_manager.data.repository.ShiftRepository;
import com.example.cafe_manager.util.RepositoryCallback;

import java.util.List;

public class ShiftTemplateViewModel extends AndroidViewModel {

    private final ShiftRepository repository;
    private final LiveData<List<ShiftTemplateEntity>> rawTemplates;
    private final MediatorLiveData<List<ShiftTemplateEntity>> filteredTemplates = new MediatorLiveData<>();
    private final MutableLiveData<String> currentFilter = new MutableLiveData<>("ALL");
    private final MutableLiveData<String> message = new MutableLiveData<>();

    public ShiftTemplateViewModel(@NonNull Application application) {
        super(application);
        this.repository = ShiftRepository.getInstance(application);
        this.rawTemplates = repository.getAllTemplates();

        filteredTemplates.addSource(rawTemplates, list -> applyFilter());
        filteredTemplates.addSource(currentFilter, filter -> applyFilter());
    }

    private void applyFilter() {
        List<ShiftTemplateEntity> list = rawTemplates.getValue();
        String filter = currentFilter.getValue();
        if (list == null) {
            filteredTemplates.setValue(null);
            return;
        }
        if (filter == null || "ALL".equals(filter)) {
            filteredTemplates.setValue(list);
        } else {
            java.util.List<ShiftTemplateEntity> filtered = new java.util.ArrayList<>();
            boolean wantActive = "ACTIVE".equals(filter);
            for (ShiftTemplateEntity t : list) {
                if (t.isActive() == wantActive) {
                    filtered.add(t);
                }
            }
            filteredTemplates.setValue(filtered);
        }
    }

    public LiveData<List<ShiftTemplateEntity>> getTemplates() { return filteredTemplates; }
    public void setFilter(String filter) { currentFilter.setValue(filter); }
    public String getFilter() { return currentFilter.getValue(); }
    public LiveData<String> getMessage() { return message; }
    public void clearMessage() { message.setValue(null); }

    public void addTemplate(String name, String start, String end, int minStaff) {
        if (name == null || name.trim().isEmpty()) {
            message.setValue("Tên mẫu ca không được trống.");
            return;
        }
        if (start.equals(end)) {
            message.setValue("Giờ bắt đầu và kết thúc không được trùng nhau.");
            return;
        }
        if (minStaff <= 0) {
            message.setValue("Số nhân viên tối thiểu phải lớn hơn 0.");
            return;
        }

        ShiftTemplateEntity template = new ShiftTemplateEntity();
        template.setTemplateName(name.trim());
        template.setStartTime(start);
        template.setEndTime(end);
        template.setMinStaff(minStaff);
        template.setActive(true);
        template.setCreatedAt(System.currentTimeMillis());

        repository.insertTemplate(template, new RepositoryCallback<Long>() {
            @Override
            public void onSuccess(Long result) {
                message.setValue("Thêm mẫu ca thành công.");
            }

            @Override
            public void onError(Exception e) {
                message.setValue("Lỗi: " + e.getMessage());
            }
        });
    }

    public void updateTemplate(ShiftTemplateEntity template) {
        repository.updateTemplate(template, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                message.setValue("Cập nhật mẫu ca thành công.");
            }

            @Override
            public void onError(Exception e) {
                message.setValue("Lỗi: " + e.getMessage());
            }
        });
    }

    public void deactivateTemplate(int templateId) {
        repository.deactivateTemplate(templateId, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                message.setValue("Đã xóa/ngưng hoạt động mẫu ca.");
            }

            @Override
            public void onError(Exception e) {
                message.setValue("Lỗi: " + e.getMessage());
            }
        });
    }
}
