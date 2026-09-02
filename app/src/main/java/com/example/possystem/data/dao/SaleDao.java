package com.example.possystem.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.possystem.data.entity.SaleEntity;
import com.example.possystem.data.entity.SaleItemEntity;

import java.util.List;

@Dao
public interface SaleDao {

    @Insert
    long insertSale(SaleEntity sale);

    @Insert
    void insertSaleItems(List<SaleItemEntity> items);

    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    List<SaleEntity> getAllSales();

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    List<SaleItemEntity> getItemsForSale(int saleId);

    @Query("SELECT SUM(total) FROM sales")
    Double getTotalSalesAmount();

    @Query("SELECT COUNT(*) FROM sales")
    int getSalesCount();

    @Query("SELECT SUM(discount) FROM sales")
    Double getTotalDiscountAmount();

    @Query("SELECT * FROM sales WHERE saleType = 'Credit' OR paymentMethod = 'Credit'")
    List<SaleEntity> getCreditSales();
}
