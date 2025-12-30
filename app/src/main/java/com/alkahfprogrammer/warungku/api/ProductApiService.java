package com.alkahfprogrammer.warungku.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Service untuk lookup produk dari database eksternal berdasarkan barcode
 * 
 * Menggunakan Open Food Facts API - 100% GRATIS, TANPA BATASAN!
 * Dokumentasi: https://world.openfoodfacts.org/data
 */
public interface ProductApiService {
    
    /**
     * Lookup produk berdasarkan barcode menggunakan Open Food Facts API
     * 
     * Endpoint: GET /product/{barcode}.json
     * 
     * ✅ 100% gratis, tidak ada batasan request
     * ✅ Tidak perlu API key
     * ✅ Database sangat besar, termasuk produk Indonesia
     * 
     * @param barcode Kode barcode produk (EAN-13, UPC, dll)
     * @return Data produk dari database eksternal
     */
    @GET("product/{barcode}.json")
    Call<ProductApiResponse> lookupProduct(@Path("barcode") String barcode);
}

