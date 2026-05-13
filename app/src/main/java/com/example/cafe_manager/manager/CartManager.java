package com.example.cafe_manager.manager;

import com.example.cafe_manager.model.CartItem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CartManager {

    private static CartManager instance;

    private int currentTableId = -1;
    private final List<CartItem> cartItems = new ArrayList<>();

    private CartManager() {
    }

    public static synchronized CartManager getInstance() {
        if (instance == null) {
            instance = new CartManager();
        }
        return instance;
    }

    // ========================
    // Table session
    // ========================

    public void setCurrentTableId(int tableId) {
        this.currentTableId = tableId;
    }

    public int getCurrentTableId() {
        return currentTableId;
    }

    public void clearCurrentTable() {
        currentTableId = -1;
    }

    // ========================
    // Cart read
    // ========================

    public List<CartItem> getItems() {
        return new ArrayList<>(cartItems);
    }

    public boolean isEmpty() {
        return cartItems.isEmpty();
    }

    public int getDistinctItemCount() {
        return cartItems.size();
    }

    public int getTotalQuantity() {
        int total = 0;

        for (CartItem item : cartItems) {
            total += item.getQuantity();
        }

        return total;
    }

    public double getTotalAmount() {
        double total = 0;

        for (CartItem item : cartItems) {
            total += item.getSubtotal();
        }

        return total;
    }

    // ========================
    // Cart mutation
    // ========================

    /**
     * Thêm sản phẩm vào giỏ hàng
     */
    public void addItem(int productId, String productName, double unitPrice) {

        CartItem existingItem = findItemByProductId(productId);

        if (existingItem != null) {
            existingItem.setQuantity(
                    existingItem.getQuantity() + 1
            );
            return;
        }

        CartItem newItem = new CartItem();

        newItem.setProductId(productId);
        newItem.setProductName(productName);
        newItem.setQuantity(1);
        newItem.setUnitPrice(unitPrice);
        newItem.setNote("");

        cartItems.add(newItem);
    }

    public void increaseQuantity(int productId) {

        CartItem item = findItemByProductId(productId);

        if (item == null) {
            return;
        }

        item.setQuantity(item.getQuantity() + 1);
    }

    public void decreaseQuantity(int productId) {

        CartItem item = findItemByProductId(productId);

        if (item == null) {
            return;
        }

        if (item.getQuantity() > 1) {
            item.setQuantity(item.getQuantity() - 1);
        } else {
            removeItem(productId);
        }
    }

    public void removeItem(int productId) {

        Iterator<CartItem> iterator = cartItems.iterator();

        while (iterator.hasNext()) {

            CartItem item = iterator.next();

            if (item.getProductId() == productId) {
                iterator.remove();
                return;
            }
        }
    }

    public void updateNote(int productId, String note) {

        CartItem item = findItemByProductId(productId);

        if (item == null) {
            return;
        }

        item.setNote(
                note == null ? "" : note.trim()
        );
    }

    public void clearCart() {
        cartItems.clear();
        currentTableId = -1;
    }

    // ========================
    // Helper
    // ========================

    private CartItem findItemByProductId(int productId) {

        for (CartItem item : cartItems) {

            if (item.getProductId() == productId) {
                return item;
            }
        }

        return null;
    }
}