package com.example.possystem.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "return_items")
public class ReturnItemEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public int returnId;
    public int productId;
    public String productName;
    public double returnedQuantity;
    public double unitPrice;
    public double lineTotal;

    public ReturnItemEntity() {}

    @Ignore
    public ReturnItemEntity(int returnId, int productId, String productName, double returnedQuantity, double unitPrice, double lineTotal) {
        this.returnId = returnId;
        this.productId = productId;
        this.productName = productName;
        this.returnedQuantity = returnedQuantity;
        this.unitPrice = unitPrice;
        this.lineTotal = lineTotal;
    }
}
