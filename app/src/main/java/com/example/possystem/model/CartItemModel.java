package com.example.possystem.model;

import com.example.possystem.data.entity.ProductEntity;

public class CartItemModel {
    public ProductEntity product;
    public double quantity;
    public double total;

    public CartItemModel(ProductEntity product, double quantity) {
        this.product = product;
        this.quantity = quantity;
        this.total = product.sellingPrice * quantity;
    }

    public void recalculate() {
        this.total = product.sellingPrice * quantity;
    }
}
