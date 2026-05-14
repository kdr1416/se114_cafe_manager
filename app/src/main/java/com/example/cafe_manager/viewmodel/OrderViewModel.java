package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.repository.OrderRepository;
import com.example.cafe_manager.manager.CartManager;
import com.example.cafe_manager.model.CartItem;
import com.example.cafe_manager.util.RepositoryCallback;

import java.util.ArrayList;
import java.util.List;

public class OrderViewModel extends AndroidViewModel {

    private final OrderRepository orderRepository;
    private final CartManager cartManager;

    private final MutableLiveData<List<CartItem>> cartItemsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> totalQuantityLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<Double> totalAmountLiveData = new MutableLiveData<>(0.0);

    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Long> confirmSuccessLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>();

    public OrderViewModel(@NonNull Application application) {
        super(application);

        this.orderRepository = new OrderRepository(application);
        this.cartManager = CartManager.getInstance();

        refreshCartState();
    }

    public LiveData<List<CartItem>> getCartItems() {
        return cartItemsLiveData;
    }

    public LiveData<Integer> getTotalQuantity() {
        return totalQuantityLiveData;
    }

    public LiveData<Double> getTotalAmount() {
        return totalAmountLiveData;
    }

    public LiveData<Boolean> getLoading() {
        return loadingLiveData;
    }

    public LiveData<Long> getConfirmSuccess() {
        return confirmSuccessLiveData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessageLiveData;
    }

    public void clearConfirmSuccess() {
        confirmSuccessLiveData.setValue(null);
    }

    public void clearErrorMessage() {
        errorMessageLiveData.setValue(null);
    }

    public void refreshCartState() {
        cartItemsLiveData.setValue(new ArrayList<>(cartManager.getItems()));
        totalQuantityLiveData.setValue(cartManager.getTotalQuantity());
        totalAmountLiveData.setValue(cartManager.getTotalAmount());
    }

    public boolean isCartEmpty() {
        return cartManager.isEmpty();
    }

    public int getCurrentTableId() {
        return cartManager.getCurrentTableId();
    }

    public void increaseQuantity(int productId) {
        cartManager.increaseQuantity(productId);
        refreshCartState();
    }

    public void decreaseQuantity(int productId) {
        cartManager.decreaseQuantity(productId);
        refreshCartState();
    }

    public void removeItem(int productId) {
        cartManager.removeItem(productId);
        refreshCartState();
    }

    public void updateItemNote(int productId, String note) {
        cartManager.updateNote(productId, note);
        refreshCartState();
    }

    public void clearCart() {
        cartManager.clearCart();
        refreshCartState();
    }

    public void confirmOrder(String note) {

        int tableId = cartManager.getCurrentTableId();
        List<CartItem> currentItems = cartManager.getItems();

        if (tableId == -1) {
            errorMessageLiveData.setValue("Chưa chọn bàn.");
            return;
        }

        if (currentItems.isEmpty()) {
            errorMessageLiveData.setValue("Giỏ hàng đang trống.");
            return;
        }

        loadingLiveData.setValue(true);

        orderRepository.confirmOrder(
                tableId,
                currentItems,
                note,
                new RepositoryCallback<Long>() {

                    @Override
                    public void onSuccess(Long orderId) {

                        loadingLiveData.setValue(false);

                        cartManager.clearCart();

                        refreshCartState();

                        confirmSuccessLiveData.setValue(orderId);
                    }

                    @Override
                    public void onError(Exception exception) {

                        loadingLiveData.setValue(false);

                        errorMessageLiveData.setValue(
                                exception != null
                                        ? exception.getMessage()
                                        : "Xác nhận order thất bại."
                        );
                    }
                }
        );
    }
}
