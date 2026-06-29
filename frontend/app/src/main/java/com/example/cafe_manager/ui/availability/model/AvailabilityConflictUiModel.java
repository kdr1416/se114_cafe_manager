package com.example.cafe_manager.ui.availability.model;

import java.util.List;

public class AvailabilityConflictUiModel {
    private String title;
    private String message;
    private List<String> conflictItems;

    public AvailabilityConflictUiModel() {}

    public AvailabilityConflictUiModel(String title, String message, List<String> conflictItems) {
        this.title = title;
        this.message = message;
        this.conflictItems = conflictItems;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<String> getConflictItems() { return conflictItems; }
    public void setConflictItems(List<String> conflictItems) { this.conflictItems = conflictItems; }
}
