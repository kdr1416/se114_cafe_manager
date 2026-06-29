package com.example.cafe_manager.data.remote;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PageResponse<T> {
    @SerializedName("content")
    private List<T> content;

    @SerializedName("totalPages")
    private int totalPages;

    @SerializedName("totalElements")
    private long totalElements;

    @SerializedName("number")
    private int number;

    @SerializedName("last")
    private boolean last;

    public PageResponse() {}

    public List<T> getContent() { return content; }
    public void setContent(List<T> content) { this.content = content; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }

    public boolean isLast() { return last; }
    public void setLast(boolean last) { this.last = last; }
}
