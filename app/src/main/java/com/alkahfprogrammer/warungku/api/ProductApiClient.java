package com.alkahfprogrammer.warungku.api;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

/**
 * Client untuk mengakses API produk eksternal
 * 
 * Catatan: 
 * - Base URL bisa diganti sesuai API yang digunakan
 * - Untuk production, tambahkan API key jika diperlukan
 * - Implementasi caching untuk mengurangi API calls
 */
public class ProductApiClient {
    
    private static final String TAG = "ProductApiClient";
    
    // Base URL - Open Food Facts API (100% GRATIS, TANPA BATASAN!)
    // Dokumentasi: https://world.openfoodfacts.org/data
    // ✅ 100% gratis, tidak ada batasan request
    // ✅ Database sangat besar, termasuk produk Indonesia
    // ✅ Open source, community-driven
    private static final String BASE_URL = "https://world.openfoodfacts.org/api/v0/";
    
    private static ProductApiService apiService;
    
    /**
     * Get API service instance
     * Menggunakan singleton pattern untuk efisiensi
     */
    public static ProductApiService getApiService() {
        if (apiService == null) {
            // Setup OkHttp client
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .build();
            
            // Setup Retrofit
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            
            apiService = retrofit.create(ProductApiService.class);
        }
        
        return apiService;
    }
    
    /**
     * Check if API is configured
     * Return true karena sudah dikonfigurasi dengan Open Food Facts API (100% gratis!)
     */
    public static boolean isConfigured() {
        return true; // API sudah dikonfigurasi - Open Food Facts 100% gratis!
    }
}

