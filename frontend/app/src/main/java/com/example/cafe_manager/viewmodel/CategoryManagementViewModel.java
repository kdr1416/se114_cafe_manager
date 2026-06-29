package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.local.entity.CategoryEntity;
import com.example.cafe_manager.data.repository.MenuRepository;

import java.util.List;

public class CategoryManagementViewModel extends AndroidViewModel {

    private final MenuRepository repository;
    private final LiveData<List<CategoryEntity>> categories;
    private final MutableLiveData<String> message = new MutableLiveData<>();

    public CategoryManagementViewModel(@NonNull Application application) {
        super(application);
        repository = MenuRepository.getInstance(application);
        categories = repository.getAllCategories();
    }

    public LiveData<List<CategoryEntity>> getCategories() {
        return categories;
    }

    public LiveData<String> getMessage() {
        return message;
    }

    public void clearMessage() {
        message.setValue(null);
    }

    public void addCategory(String name, String description) {
        CategoryEntity category = new CategoryEntity(name, true, description);
        repository.insertCategory(category, () -> message.setValue("Thêm danh mục thành công"));
    }

    public void updateCategory(CategoryEntity category) {
        repository.updateCategory(category, () -> message.setValue("Cập nhật danh mục thành công"));
    }

    public void deleteCategory(int categoryId) {
        repository.deleteCategory(
                categoryId,
                () -> message.setValue("Xoá danh mục thành công"),
                () -> message.setValue("Không thể xoá danh mục đang chứa món ăn")
        );
    }
}
