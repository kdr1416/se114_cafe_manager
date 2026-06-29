package com.example.cafe_manager.model;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.example.cafe_manager.data.local.entity.CategoryEntity;
import com.example.cafe_manager.data.local.entity.ProductEntity;

public class ProductWithCategory {

    @Embedded
    private ProductEntity product;

    @Relation(
            parentColumn = "category_id",
            entityColumn = "category_id"
    )
    private CategoryEntity category;

    public ProductEntity getProduct() {
        return product;
    }

    public void setProduct(ProductEntity product) {
        this.product = product;
    }

    public CategoryEntity getCategory() {
        return category;
    }

    public void setCategory(CategoryEntity category) {
        this.category = category;
    }
}
