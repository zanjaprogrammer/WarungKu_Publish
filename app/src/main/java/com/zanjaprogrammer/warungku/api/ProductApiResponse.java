package com.zanjaprogrammer.warungku.api;

import com.google.gson.annotations.SerializedName;

/**
 * Response model dari Open Food Facts API
 * Format response sesuai dengan dokumentasi: https://world.openfoodfacts.org/data
 * 
 * ✅ 100% gratis, tidak ada batasan request
 * ✅ Database sangat besar, termasuk produk Indonesia
 */
public class ProductApiResponse {
    
    @SerializedName("status")
    public Integer status; // 1 = found, 0 = not found
    
    @SerializedName("status_verbose")
    public String statusVerbose; // Status message
    
    @SerializedName("product")
    public Product product;
    
    /**
     * Nested class untuk product data dari Open Food Facts
     */
    public static class Product {
        @SerializedName("product_name")
        public String productName; // Nama produk
        
        @SerializedName("product_name_en")
        public String productNameEn; // Nama produk (English)
        
        @SerializedName("brands")
        public String brands; // Merek (bisa multiple, dipisah koma)
        
        @SerializedName("brands_tags")
        public String[] brandsTags; // Array brand tags
        
        @SerializedName("categories")
        public String categories; // Kategori
        
        @SerializedName("categories_tags")
        public String[] categoriesTags; // Array kategori tags
        
        @SerializedName("quantity")
        public String quantity; // Kuantitas (misal: "330ml", "500g")
        
        @SerializedName("image_url")
        public String imageUrl; // URL gambar produk
        
        @SerializedName("image_front_url")
        public String imageFrontUrl; // URL gambar depan
        
        @SerializedName("image_small_url")
        public String imageSmallUrl; // URL gambar kecil
        
        @SerializedName("code")
        public String code; // Barcode
        
        @SerializedName("countries")
        public String countries; // Negara (bisa multiple)
        
        @SerializedName("countries_tags")
        public String[] countriesTags; // Array negara tags
        
        @SerializedName("ingredients_text")
        public String ingredientsText; // Daftar bahan
        
        @SerializedName("nutriments")
        public Nutriments nutriments; // Informasi nutrisi
    }
    
    /**
     * Nested class untuk nutriments (opsional, untuk info nutrisi)
     */
    public static class Nutriments {
        @SerializedName("energy-kcal_100g")
        public Double energyKcal; // Kalori per 100g
    }
    
    public ProductApiResponse() {
    }
    
    /**
     * Check if response is successful
     */
    public boolean isSuccess() {
        try {
            return status != null && status == 1 && product != null && 
                   ((product.productName != null && !product.productName.isEmpty()) ||
                    (product.productNameEn != null && !product.productNameEn.isEmpty()));
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get product name (prioritize Indonesian name, fallback to English)
     */
    public String getName() {
        try {
            if (product != null) {
                // Prioritas: productName (biasanya bahasa lokal) -> productNameEn
                if (product.productName != null && !product.productName.trim().isEmpty()) {
                    return product.productName.trim();
                } else if (product.productNameEn != null && !product.productNameEn.trim().isEmpty()) {
                    return product.productNameEn.trim();
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get brand name
     */
    public String getBrand() {
        try {
            if (product != null && product.brands != null && !product.brands.trim().isEmpty()) {
                // Ambil brand pertama jika ada multiple (dipisah koma)
                String[] brands = product.brands.split(",");
                return brands[0].trim();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get category
     */
    public String getCategory() {
        try {
            if (product != null && product.categories != null && !product.categories.trim().isEmpty()) {
                // Ambil kategori pertama jika ada multiple (dipisah koma)
                String[] categories = product.categories.split(",");
                return categories[0].trim();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get quantity/size
     */
    public String getQuantity() {
        try {
            if (product != null && product.quantity != null && !product.quantity.trim().isEmpty()) {
                return product.quantity.trim();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get best available image URL (prioritize front image, fallback to general image)
     */
    public String getImageUrl() {
        try {
            if (product != null) {
                // Priority: imageFrontUrl -> imageUrl -> imageSmallUrl
                if (product.imageFrontUrl != null && !product.imageFrontUrl.trim().isEmpty()) {
                    return product.imageFrontUrl.trim();
                } else if (product.imageUrl != null && !product.imageUrl.trim().isEmpty()) {
                    return product.imageUrl.trim();
                } else if (product.imageSmallUrl != null && !product.imageSmallUrl.trim().isEmpty()) {
                    return product.imageSmallUrl.trim();
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}