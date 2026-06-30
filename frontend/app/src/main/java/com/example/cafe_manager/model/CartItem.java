package com.example.cafe_manager.model;

public class CartItem {
    private int productId;
    private String productName;
    private int quantity;
    private double unitPrice;
    private String note;

    public CartItem() {}

    public CartItem(int productId, String productName, int quantity, double unitPrice, String note) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.note = note;
    }

    public double getSubtotal() {
        return quantity * unitPrice;
    }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}