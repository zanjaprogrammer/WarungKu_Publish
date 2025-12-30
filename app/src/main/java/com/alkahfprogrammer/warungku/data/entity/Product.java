package com.alkahfprogrammer.warungku.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "products")
public class Product {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public double sellPrice;
    public Double buyPrice; // Nullable
    public int currentStock;
    public int minStock;
    public int salesCount; // To track frequency
    public boolean isFavorite; // Favorite flag
    public long lastSoldTimestamp; // Last sold timestamp
    public String barcode; // Barcode for scanning (nullable)
    public String imageUrl; // Product image URL (nullable)

    public Product(String name, double sellPrice, Double buyPrice, int currentStock, int minStock) {
        this.name = name;
        this.sellPrice = sellPrice;
        this.buyPrice = buyPrice;
        this.currentStock = currentStock;
        this.minStock = minStock;
        this.salesCount = 0;
        this.isFavorite = false;
        this.lastSoldTimestamp = 0;
        this.barcode = null;
        this.imageUrl = null;
    }
}
