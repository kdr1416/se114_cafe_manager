package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.cafe_manager.data.local.entity.CategoryEntity;
import com.example.cafe_manager.data.local.entity.ProductEntity;
import com.example.cafe_manager.data.repository.MenuRepository;

import java.util.List;

public class AdminMenuViewModel extends AndroidViewModel {

    private final MenuRepository menuRepository;

    private final MutableLiveData<Integer> selectedCategoryIdLive =
            new MutableLiveData<>(0);  // 0 = Tất cả

    private final LiveData<List<ProductEntity>> productsLive;
    private final LiveData<List<CategoryEntity>> categoriesLive;
    private final LiveData<Integer> totalCountLive;
    private final LiveData<Integer> visibleCountLive;

    public AdminMenuViewModel(@NonNull Application application) {
        super(application);

        this.menuRepository = MenuRepository.getInstance(application);
        this.categoriesLive = menuRepository.getActiveCategories();

        this.productsLive = Transformations.switchMap(selectedCategoryIdLive, id -> {
            if (id == null || id == 0) {
                return menuRepository.getAllProducts();
            }
            return menuRepository.getProductsByCategoryIncludingInactive(id);
        });

        this.totalCountLive = Transformations.map(
                productsLive,
                list -> list == null ? 0 : list.size()
        );

        this.visibleCountLive = Transformations.map(
                productsLive,
                list -> {
                    if (list == null) return 0;
                    int n = 0;
                    for (ProductEntity p : list) {
                        if (p.isActive()) n++;
                    }
                    return n;
                }
        );
    }

    public LiveData<List<ProductEntity>> getProducts() {
        return productsLive;
    }

    public LiveData<List<CategoryEntity>> getCategories() {
        return categoriesLive;
    }

    public LiveData<Integer> getTotalCount() {
        return totalCountLive;
    }

    public LiveData<Integer> getVisibleCount() {
        return visibleCountLive;
    }

    public int getSelectedCategoryId() {
        Integer v = selectedCategoryIdLive.getValue();
        return v == null ? 0 : v;
    }

    public void selectCategory(int categoryId) {
        Integer current = selectedCategoryIdLive.getValue();
        if (current != null && current == categoryId) return;
        selectedCategoryIdLive.setValue(categoryId);
    }

    public void addProduct(int categoryId, String name, double price) {
        ProductEntity p = new ProductEntity();
        p.setCategoryId(categoryId);
        p.setProductName(name);
        p.setPrice(price);
        p.setActive(true);
        p.setCreatedAt(System.currentTimeMillis());
        menuRepository.insertProduct(p);
    }

    public void updateProduct(ProductEntity product) {
        menuRepository.updateProduct(product);
    }

    public void toggleActive(int productId, boolean newActive) {
        menuRepository.updateProductActiveStatus(productId, newActive);
    }
}
