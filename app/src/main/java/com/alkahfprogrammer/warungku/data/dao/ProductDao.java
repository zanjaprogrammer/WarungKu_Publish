package com.alkahfprogrammer.warungku.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.alkahfprogrammer.warungku.data.entity.Product;

import java.util.List;

@Dao
public interface ProductDao {
    @Insert
    long insert(Product product);

    @Update
    void update(Product product);

    @Delete
    void delete(Product product);

    @Query("SELECT * FROM products ORDER BY name ASC")
    LiveData<List<Product>> getAllProducts();
    
    @Query("SELECT * FROM products ORDER BY name ASC")
    List<Product> getAllProductsSync();

    @Query("SELECT * FROM products WHERE currentStock <= minStock ORDER BY salesCount DESC")
    LiveData<List<Product>> getShoppingList();

    @Query("SELECT * FROM products WHERE id = :id")
    Product getProductById(int id);

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    Product getProductByBarcode(String barcode);

    // Query untuk laporan: Produk terlaris (top 10 berdasarkan salesCount)
    @Query("SELECT * FROM products WHERE salesCount > 0 ORDER BY salesCount DESC LIMIT :limit")
    LiveData<List<Product>> getTopSellingProducts(int limit);

    // Query untuk laporan: Produk tidak laku (belum pernah dijual)
    @Query("SELECT * FROM products WHERE salesCount = 0 OR salesCount IS NULL ORDER BY name ASC")
    LiveData<List<Product>> getUnsoldProducts();
    
    @Query("DELETE FROM products")
    void deleteAll();
}
