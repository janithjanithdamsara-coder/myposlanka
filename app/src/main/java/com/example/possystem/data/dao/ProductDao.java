package com.example.possystem.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.possystem.data.entity.ProductEntity;

import java.util.List;

@Dao
public interface ProductDao {

    @Insert
    long insert(ProductEntity product);

    @Update
    void update(ProductEntity product);

    @Delete
    void delete(ProductEntity product);

    @Query("DELETE FROM products WHERE id = :id")
    void deleteById(int id);

    @Query("SELECT * FROM products ORDER BY name ASC")
    List<ProductEntity> getAllProducts();

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    ProductEntity getProductByBarcode(String barcode);

    @Query("SELECT * FROM products WHERE stockQuantity <= minimumStock")
    List<ProductEntity> getLowStockProducts();

    @Query("SELECT * FROM products WHERE expiryDate IS NOT NULL AND expiryDate != ''")
    List<ProductEntity> getExpiringProducts();

    @Query("SELECT SUM(stockQuantity * purchasePrice) FROM products")
    Double getTotalStockValue();

    @Query("SELECT COUNT(*) FROM products WHERE stockQuantity <= minimumStock")
    int getLowStockCount();

    @Query("SELECT COUNT(*) FROM products WHERE expiryDate IS NOT NULL AND expiryDate != ''")
    int getExpiringCount();

    @Query("UPDATE products SET stockQuantity = stockQuantity - :quantity WHERE id = :productId")
    void reduceStock(int productId, double quantity);

    @Query("UPDATE products SET stockQuantity = :newStock WHERE id = :productId")
    void updateStockQuantity(int productId, double newStock);
}
