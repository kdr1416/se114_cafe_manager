package com.example.cafe_manager.data.remote;

public class OrderRequest {
    private int tableId;
    private String note;

    public OrderRequest(int tableId, String note) {
        this.tableId = tableId;
        this.note = note;
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
}
