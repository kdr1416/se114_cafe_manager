package com.example.cafe_manager.data.remote;

import java.util.List;

public class OrderRequest {
    private int tableId;
    private String note;
    private List<OrderItemRequest> items;

    public OrderRequest(int tableId, String note) {
        this.tableId = tableId;
        this.note = note;
    }

    public OrderRequest(int tableId, String note, List<OrderItemRequest> items) {
        this.tableId = tableId;
        this.note = note;
        this.items = items;
    }

    public int getTableId() {
        return tableId;
    }

    public void setTableId(int tableId) {
        this.tableId = tableId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }
}
