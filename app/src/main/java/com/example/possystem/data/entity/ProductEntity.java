package com.example.possystem.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "products")
public class ProductEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String barcode;
    public String name;
    public double purchasePrice;
    public double sellingPrice;
    public String unit; // "pcs", "kg", "g"
    public boolean isWeightBased;
    public double stockQuantity;
    public double minimumStock;
    public String expiryDate;

    // Default Constructor for Room
    public ProductEntity() {}

    // Main Constructor
    @Ignore
    public ProductEntity(String barcode, String name, double purchasePrice, double sellingPrice, 
                         String unit, boolean isWeightBased, double stockQuantity, 
                         double minimumStock, String expiryDate) {
        this.barcode = barcode;
        this.name = name;
        this.purchasePrice = purchasePrice;
        this.sellingPrice = sellingPrice;
        this.unit = unit;
        this.isWeightBased = isWeightBased;
        this.stockQuantity = stockQuantity;
        this.minimumStock = minimumStock;
        this.expiryDate = expiryDate;
    }
}