package com.example.cafe_manager.model;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.example.cafe_manager.data.local.entity.OrderEntity;
import com.example.cafe_manager.data.local.entity.OrderItemEntity;

import java.util.List;

public class OrderWithItems {

    @Embedded
    private OrderEntity order;

    @Relation(
            parentColumn = "order_id",
            entityColumn = "order_id"
    )
    private List<OrderItemEntity> items;

    @Relation(
            parentColumn = "table_id",
            entityColumn = "table_id",
            entity = com.example.cafe_manager.data.local.entity.TableEntity.class,
            projection = {"table_name"}
    )
    private String tableName;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public OrderEntity getOrder() {
        return order;
    }

    public void setOrder(OrderEntity order) {
        this.order = order;
    }

    public List<OrderItemEntity> getItems() {
        return items;
    }

    public void setItems(List<OrderItemEntity> items) {
        this.items = items;
    }
}
