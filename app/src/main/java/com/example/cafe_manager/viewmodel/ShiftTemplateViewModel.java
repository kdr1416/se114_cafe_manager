package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.local.entity.ShiftTemplateEntity;
import com.example.cafe_manager.data.repository.ShiftRepository;
import com.example.cafe_manager.util.RepositoryCallback;

import java.util.List;

public class ShiftTemplateViewModel extends AndroidViewModel {

    private final ShiftRepository repository;
    private final LiveData<List<ShiftTemplateEntity>> templates;
    private final MutableLiveData<String> message = new MutableLiveData<>();

    public ShiftTemplateViewModel(@NonNull Application application) {
        super(application);
        this.repository = new ShiftRepository(application);
        this.templates = repository.getAllTemplates();
    }

    public LiveData<List<ShiftTemplateEntity>> getTemplates() { return templates; }
    public LiveData<String> getMessage() { return message; }
    public void clearMessage() { message.setValue(null); }

    public void addTemplate(String name, String start, String end, int minStaff) {
        if (name == null || name.trim().isEmpty()) {
            message.setValue("Tên mẫu ca không được trống.");
            return;
        }
        if (start.compareTo(end) >= 0) {
            message.setValue("Giờ bắt đầu phải trước giờ kết thúc.");
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
