package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.local.entity.CategoryEntity;
import com.example.cafe_manager.data.local.entity.ProductEntity;
import com.example.cafe_manager.data.repository.MenuRepository;
import com.example.cafe_manager.manager.CartManager;

import java.util.List;

public class MenuViewModel extends AndroidViewModel {

    private final MenuRepository menuRepository;
    private final CartManager cartManager;

    private final MutableLiveData<Integer> cartTotalQuantityLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<Double> cartTotalAmountLiveData = new MutableLiveData<>(0.0);

    public MenuViewModel(@NonNull Application application) {
        super(application);
        this.menuRepository = new MenuRepository(application);
        this.cartManager = CartManager.getInstance();
        refreshCartSummary();
    }

    public LiveData<List<CategoryEntity>> getCategories() {
        return menuRepository.getActiveCategories();
    }

    public LiveData<List<ProductEntity>> getAllProducts() {
        return menuRepository.getActiveProducts();
    }

    public LiveData<List<ProductEntity>> getProductsByCategory(int categoryId) {
        return menuRepository.getProductsByCategory(categoryId);
    }

    public void setCurrentTable(int tableId) {
        cartManager.setCurrentTableId(tableId);
        refreshCartSummary();
    }

    public int getCurrentTableId() {
        return cartManager.getCurrentTableId();
    }

    public LiveData<Integer> getCartTotalQuantityLiveData() {
        return cartTotalQuantityLiveData;
    }

    public LiveData<Double> getCartTotalAmountLiveData() {
        return cartTotalAmountLiveData;
    }

    private void refreshCartSummary() {
        cartTotalQuantityLiveData.setValue(cartManager.getTotalQuantity());
        cartTotalAmountLiveData.setValue(cartManager.getTotalAmount());
    }

    public void addToCart(ProductEntity product) {
        if (product == null) {
            return;
        }

        cartManager.addItem(
                product.getProductId(),
                product.getProductName(),
                product.getPrice()
        );

        refreshCartSummary();
    }

    public void refreshCartBar() {
        refreshCartSummary();
    }
}
