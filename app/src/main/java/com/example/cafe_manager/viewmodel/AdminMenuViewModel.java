package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.entity.ProductEntity;
import com.example.cafe_manager.data.repository.MenuRepository;

import java.util.List;

public class AdminMenuViewModel extends AndroidViewModel {

    private final MenuRepository repository;
    private final LiveData<List<ProductEntity>> products;

    public AdminMenuViewModel(@NonNull Application application) {
        super(application);

        repository = new MenuRepository(application);

        products = repository.getAllProducts();
    }

    public LiveData<List<ProductEntity>> getProducts() {
        return products;
    }

    public void updateProduct(ProductEntity product) {
        repository.updateProduct(product);
    }

    public void addProduct(
            int categoryId,
            String name,
            double price
    ) {

        ProductEntity product = new ProductEntity();

        product.setCategoryId(categoryId);
        product.setProductName(name);
        product.setPrice(price);
        product.setActive(true);

        repository.insertProduct(product);
    }
}
