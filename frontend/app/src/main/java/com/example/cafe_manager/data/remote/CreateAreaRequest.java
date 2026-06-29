package com.example.cafe_manager.data.remote;

public class CreateAreaRequest {
    private String areaName;
    private String prefix;
    private String description;

    public CreateAreaRequest() {}

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}