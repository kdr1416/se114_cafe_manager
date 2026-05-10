package com.example.cafe_manager.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.dao.CategoryDao;
import com.example.cafe_manager.data.local.dao.ProductDao;
import com.example.cafe_manager.data.local.entity.CategoryEntity;
import com.example.cafe_manager.data.local.entity.ProductEntity;
import com.example.cafe_manager.util.AppExecutors;

import java.util.List;

public class MenuRepository {

    private final CategoryDao categoryDao;
    private final ProductDao productDao;
    private final AppExecutors appExecutors;

    public MenuRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.categoryDao = db.categoryDao();
        this.productDao = db.productDao();
        this.appExecutors = AppExecutors.getInstance();
    }

    public LiveData<List<CategoryEntity>> getActiveCategories() {
        return categoryDao.getAllActive();
    }

    public LiveData<List<ProductEntity>> getActiveProducts() {
        return productDao.getAllActive();
    }

    public LiveData<List<ProductEntity>> getProductsByCategory(int categoryId) {
        return productDao.getByCategoryId(categoryId);
    }

    public void insertProduct(ProductEntity product) {
        appExecutors.diskIO().execute(() ->
                productDao.insert(product)
        );
    }

    public void updateProduct(ProductEntity product) {
        appExecutors.diskIO().execute(() ->
                productDao.update(product)
        );
    }

    public void updateProductActiveStatus(int productId, boolean isActive) {
        appExecutors.diskIO().execute(() ->
                productDao.setActive(productId, isActive)
        );
    }
}