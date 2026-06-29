package com.example.cafe_manager.data.remote;

public class LeaveReviewRequest {
    private String reviewNote;

    public LeaveReviewRequest() {}

    public LeaveReviewRequest(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }
}
