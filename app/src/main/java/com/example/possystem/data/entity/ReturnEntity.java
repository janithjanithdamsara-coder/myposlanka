package com.example.possystem.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "returns")
public class ReturnEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String returnInvoiceNumber;
    public String originalInvoiceNumber;
    public double refundTotal;
    public String reason;
    public long timestamp;

    public ReturnEntity() {}

    @Ignore
    public ReturnEntity(String returnInvoiceNumber, String originalInvoiceNumber, double refundTotal, String reason) {
        this.returnInvoiceNumber = returnInvoiceNumber;
        this.originalInvoiceNumber = originalInvoiceNumber;
        this.refundTotal = refundTotal;
        this.reason = reason;
        this.timestamp = System.currentTimeMillis();
    }
}
