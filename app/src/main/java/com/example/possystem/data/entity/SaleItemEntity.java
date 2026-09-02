package com.example.possystem.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "sale_items")
public class SaleItemEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public int saleId;
    public int productId;
    public String productName;
    public double quantity;
    public double unitPrice;
    public double total;

    public SaleItemEntity() {}

    @Ignore
    public SaleItemEntity(int saleId, int productId, String productName, double quantity, double unitPrice, double total) {
        this.saleId = saleId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.total = total;
    }
}
