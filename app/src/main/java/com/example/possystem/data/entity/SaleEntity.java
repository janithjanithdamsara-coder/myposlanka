package com.example.possystem.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "sales")
public class SaleEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String invoiceNumber;
    public double subtotal;
    public double discount;
    public double total;
    public double paidAmount;
    public double changeAmount;
    public String paymentMethod; // "Cash", "Card", "QR", "Credit"
    public String saleType;      // "Cash", "Credit"
    public long timestamp;

    public SaleEntity() {}

    @Ignore
    public SaleEntity(String invoiceNumber, double subtotal, double discount, double total, 
                      double paidAmount, double changeAmount, String paymentMethod, String saleType) {
        this.invoiceNumber = invoiceNumber;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.paidAmount = paidAmount;
        this.changeAmount = changeAmount;
        this.paymentMethod = paymentMethod;
        this.saleType = saleType;
        this.timestamp = System.currentTimeMillis();
    }
}
