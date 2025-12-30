package com.zanjaprogrammer.warungku;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.journeyapps.barcodescanner.ScanOptions;
import com.journeyapps.barcodescanner.ScanContract;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.zanjaprogrammer.warungku.adapters.ProductSellAdapter;
import com.zanjaprogrammer.warungku.data.entity.Product;
import com.zanjaprogrammer.warungku.databinding.ActivitySellBinding;
import com.zanjaprogrammer.warungku.utils.BarcodeScannerHelper;
import com.zanjaprogrammer.warungku.utils.NetworkUtils;
import com.zanjaprogrammer.warungku.viewmodel.AppViewModel;
import com.zanjaprogrammer.warungku.api.ProductApiClient;
import com.zanjaprogrammer.warungku.api.ProductApiService;
import com.zanjaprogrammer.warungku.api.ProductApiResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SellActivity extends AppCompatActivity {

    private ActivitySellBinding binding;
    private AppViewModel viewModel;
    private ProductSellAdapter adapter;
    private List<Product> allProducts = new ArrayList<>();
    private String currentSearchQuery = "";
    private ActivityResultLauncher<ScanOptions> barcodeLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        binding = ActivitySellBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Use singleton instance untuk persist cart across activities
        viewModel = AppViewModel.getInstance(getApplication());
        
        // Setup barcode launcher - HARUS diinisialisasi PERTAMA sebelum digunakan
        barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result != null && result.getContents() != null) {
                String barcode = result.getContents();
                handleBarcodeScanned(barcode);
            } else {
                // User cancelled or error
                Toast.makeText(this, "Scan dibatalkan", Toast.LENGTH_SHORT).show();
            }
        });

        // Setup permission launcher
        requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    scanBarcode();
                } else {
                    Toast.makeText(this, "Izin kamera diperlukan untuk scan barcode", Toast.LENGTH_SHORT).show();
                }
            }
        );

        setupRecyclerView();
        setupSearch();
        setupBarcodeScanner();
        setupOfflineIndicator();
        
        // Setup toolbar menu dengan custom view
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_scan_barcode) {
                scanBarcode();
                return true;
            }
            return false;
        });
        
        // Setup click listener untuk custom view barcode scanner
        // Gunakan post() untuk memastikan menu sudah di-inflate
        binding.toolbar.post(() -> {
            android.view.MenuItem menuItem = binding.toolbar.getMenu().findItem(R.id.menu_scan_barcode);
            if (menuItem != null) {
                android.view.View actionView = menuItem.getActionView();
                if (actionView != null) {
                    actionView.setOnClickListener(v -> scanBarcode());
                }
            }
        });

        viewModel.getAllProducts().observe(this, products -> {
            allProducts = products != null ? products : new ArrayList<>();
            applySortingAndFilter();
        });

        binding.bottomNavigation.setSelectedItemId(R.id.nav_sell);
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                finish();
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_stock) {
                startActivity(new android.content.Intent(this, StockActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_money) {
                startActivity(new android.content.Intent(this, HistoryActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_summary) {
                startActivity(new android.content.Intent(this, SummaryActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.bottomNavigation.setSelectedItemId(R.id.nav_sell);
        setupOfflineIndicator();
    }
    
    private void setupOfflineIndicator() {
        android.content.SharedPreferences prefs = getSharedPreferences("WarungKuPrefs", MODE_PRIVATE);
        boolean hideOfflineWarning = prefs.getBoolean("hide_offline_warning", false);
        
        if (hideOfflineWarning) {
            return;
        }
        
        android.view.View includeView = findViewById(R.id.offlineIndicator);
        if (includeView == null) return;
        
        com.google.android.material.card.MaterialCardView cardOffline = (com.google.android.material.card.MaterialCardView) includeView;
        
        boolean isOnline = com.zanjaprogrammer.warungku.utils.NetworkUtils.isNetworkAvailable(this);
        
        if (!isOnline) {
            cardOffline.setVisibility(android.view.View.VISIBLE);
            
            android.view.View btnClose = cardOffline.findViewById(R.id.btnCloseOfflineIndicator);
            if (btnClose != null) {
                btnClose.setOnClickListener(v -> {
                    cardOffline.setVisibility(android.view.View.GONE);
                    prefs.edit().putBoolean("hide_offline_warning", true).apply();
                });
            }
        } else {
            cardOffline.setVisibility(android.view.View.GONE);
        }
    }

    private void setupRecyclerView() {
        adapter = new ProductSellAdapter(new ProductSellAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                if (product.currentStock > 0) {
                    showQuantityBottomSheet(product);
                } else {
                    Toast.makeText(SellActivity.this, "Stok habis!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onProductLongClick(Product product) {
                // Toggle favorite
                product.isFavorite = !product.isFavorite;
                viewModel.updateProduct(product);
                Toast.makeText(SellActivity.this, 
                    product.isFavorite ? "Ditandai favorit" : "Favorit dihapus", 
                    Toast.LENGTH_SHORT).show();
            }
        });
        binding.rvProducts.setLayoutManager(new LinearLayoutManager(this));
        binding.rvProducts.setAdapter(adapter);

        // Observe Cart
        java.text.NumberFormat formatter = java.text.NumberFormat
                .getCurrencyInstance(java.util.Locale.forLanguageTag("id-ID"));
        viewModel.getCartItems().observe(this, items -> {
            if (items != null && !items.isEmpty()) {
                binding.cardCartSummary.setVisibility(android.view.View.VISIBLE);
                // Hitung total quantity (bukan jumlah tipe produk)
                int totalQty = 0;
                for (com.zanjaprogrammer.warungku.data.model.CartItem item : items) {
                    totalQty += item.quantity;
                }
                binding.tvCartCount.setText(totalQty + " Barang");
            } else {
                binding.cardCartSummary.setVisibility(android.view.View.GONE);
            }
        });

        viewModel.getCartTotal().observe(this, total -> {
            binding.tvCartTotal.setText(formatter.format(total));
        });

        binding.btnCheckout.setOnClickListener(v -> {
            showPaymentBottomSheet();
        });
    }

    private int currentSheetQty = 1;

    private void showQuantityBottomSheet(Product product) {
        if (product == null) {
            Toast.makeText(this, "Produk tidak valid", Toast.LENGTH_SHORT).show();
            return;
        }
        
        currentSheetQty = 1;
        try {
            com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(
                    this);
            android.view.View view = getLayoutInflater().inflate(R.layout.layout_bottom_sheet_quantity, null);
            dialog.setContentView(view);

            android.widget.TextView tvName = view.findViewById(R.id.tvSheetName);
            android.widget.TextView tvPrice = view.findViewById(R.id.tvSheetPrice);
            android.widget.TextView tvQty = view.findViewById(R.id.tvSheetQuantity);
            android.view.View btnMinus = view.findViewById(R.id.btnSheetMinus);
            android.view.View btnPlus = view.findViewById(R.id.btnSheetPlus);
            android.widget.Button btnAdd = view.findViewById(R.id.btnSheetAdd);

            // Null checks untuk semua views
            if (tvName == null || tvPrice == null || tvQty == null || 
                btnMinus == null || btnPlus == null || btnAdd == null) {
                Toast.makeText(this, "Error: Layout tidak lengkap", Toast.LENGTH_SHORT).show();
                return;
            }

            java.text.NumberFormat formatter = java.text.NumberFormat
                    .getCurrencyInstance(java.util.Locale.forLanguageTag("id-ID"));
            tvName.setText(product.name != null ? product.name : "Produk");
            tvPrice.setText(formatter.format(product.sellPrice) + " (Stok: " + product.currentStock + ")");
            tvQty.setText(String.valueOf(currentSheetQty));

            btnMinus.setOnClickListener(v -> {
                if (currentSheetQty > 1) {
                    currentSheetQty--;
                    tvQty.setText(String.valueOf(currentSheetQty));
                }
            });

            btnPlus.setOnClickListener(v -> {
                if (currentSheetQty < product.currentStock) {
                    currentSheetQty++;
                    tvQty.setText(String.valueOf(currentSheetQty));
                } else {
                    Toast.makeText(this, "Mencapai batas stok!", Toast.LENGTH_SHORT).show();
                }
            });

            btnAdd.setOnClickListener(v -> {
                if (viewModel != null) {
                    viewModel.addToCart(product, currentSheetQty);
                    Toast.makeText(this, "Berhasil masuk keranjang", Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
            });

            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupSearch() {
        binding.searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().toLowerCase().trim();
                applySortingAndFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void applySortingAndFilter() {
        List<Product> filtered = new ArrayList<>();
        
        // Filter by search query
        if (currentSearchQuery.isEmpty()) {
            filtered.addAll(allProducts);
        } else {
            for (Product product : allProducts) {
                if (product.name.toLowerCase().contains(currentSearchQuery)) {
                    filtered.add(product);
                }
            }
        }

        // Sort: Favorite first, then by salesCount (desc), then by lastSoldTimestamp (desc)
        Collections.sort(filtered, new Comparator<Product>() {
            @Override
            public int compare(Product p1, Product p2) {
                // 1. Favorite first
                if (p1.isFavorite && !p2.isFavorite) return -1;
                if (!p1.isFavorite && p2.isFavorite) return 1;
                
                // 2. Then by salesCount (descending)
                int salesCompare = Integer.compare(p2.salesCount, p1.salesCount);
                if (salesCompare != 0) return salesCompare;
                
                // 3. Then by lastSoldTimestamp (descending - most recent first)
                return Long.compare(p2.lastSoldTimestamp, p1.lastSoldTimestamp);
            }
        });

        adapter.setProducts(filtered);
    }

    private void setupBarcodeScanner() {
        // FAB scan button sudah di-setup di onCreate
    }

    private void scanBarcode() {
        // Pastikan barcodeLauncher sudah diinisialisasi
        if (barcodeLauncher == null) {
            Toast.makeText(this, "Scanner belum siap", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!BarcodeScannerHelper.hasCameraPermission(this)) {
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA);
            return;
        }

        try {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES);
            options.setPrompt("Arahkan kamera ke barcode");
            options.setCameraId(0);
            options.setBeepEnabled(true);
            options.setBarcodeImageEnabled(false);
            // Lock orientasi ke portrait, tapi tetap bisa detect barcode landscape
            options.setOrientationLocked(true);
            // Set custom capture activity untuk portrait mode
            options.setCaptureActivity(PortraitCaptureActivity.class);

            barcodeLauncher.launch(options);
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void handleBarcodeScanned(String barcode) {
        // 1. Cek database lokal dulu
        Product foundProduct = null;
        for (Product product : allProducts) {
            if (product.barcode != null && product.barcode.equals(barcode)) {
                foundProduct = product;
                break;
            }
        }

        if (foundProduct != null) {
            // Produk ditemukan di database lokal
            if (foundProduct.currentStock > 0) {
                // Langsung tambah ke keranjang dengan quantity 1 (tanpa konfirmasi)
                viewModel.addToCart(foundProduct, 1);
                Toast.makeText(this, foundProduct.name + " ditambahkan ke keranjang", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Stok habis untuk " + foundProduct.name, Toast.LENGTH_SHORT).show();
            }
        } else {
            // Produk tidak ditemukan di lokal, cek API atau tanya user
            showProductNotFoundDialog(barcode);
        }
    }
    
    /**
     * Dialog ketika produk tidak ditemukan
     * Tawarkan opsi: Tambah produk baru atau cek API
     */
    private void showProductNotFoundDialog(String barcode) {
        new AlertDialog.Builder(this)
            .setTitle("Produk Tidak Ditemukan")
            .setMessage("Barcode: " + barcode + 
                "\n\nProduk belum terdaftar di database lokal.")
            .setPositiveButton("Tambah Produk", (dialog, which) -> {
                // Buka AddProductActivity dengan barcode pre-filled
                Intent intent = new Intent(this, AddProductActivity.class);
                intent.putExtra("barcode", barcode);
                startActivity(intent);
            })
            .setNeutralButton("Cek Database Eksternal", (dialog, which) -> {
                // Cek API eksternal (jika dikonfigurasi)
                if (ProductApiClient.isConfigured()) {
                    lookupProductFromApi(barcode);
                } else {
                    Toast.makeText(this, 
                        "Database eksternal belum dikonfigurasi", 
                        Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Batal", null)
            .show();
    }
    
    /**
     * Lookup produk dari API eksternal
     */
    private void lookupProductFromApi(String barcode) {
        // Check network connectivity sebelum call API
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, 
                "Tidak ada koneksi internet. Produk tidak ditemukan di database lokal.", 
                Toast.LENGTH_LONG).show();
            return;
        }
        
        Toast.makeText(this, "Mencari di database eksternal...", Toast.LENGTH_SHORT).show();
        
        ProductApiService apiService = ProductApiClient.getApiService();
        Call<ProductApiResponse> call = apiService.lookupProduct(barcode); // Open Food Facts - 100% gratis
        
        call.enqueue(new Callback<ProductApiResponse>() {
            @Override
            public void onResponse(Call<ProductApiResponse> call, Response<ProductApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ProductApiResponse apiProduct = response.body();
                    
                    if (apiProduct.isSuccess()) {
                        // Produk ditemukan di API, tawarkan untuk tambah ke database
                        showAddProductFromApiDialog(apiProduct, barcode);
                    } else {
                        Toast.makeText(SellActivity.this, 
                            "Produk tidak ditemukan di database eksternal", 
                            Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(SellActivity.this, 
                        "Gagal mengakses database eksternal", 
                        Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ProductApiResponse> call, Throwable t) {
                Toast.makeText(SellActivity.this, 
                    "Tidak dapat mengakses database eksternal", 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Dialog untuk menambah produk dari API ke database lokal
     */
    private void showAddProductFromApiDialog(ProductApiResponse apiProduct, String barcode) {
        String productName = apiProduct.getName();
        String brand = apiProduct.getBrand();
        String quantity = apiProduct.getQuantity();
        String category = apiProduct.getCategory();
        
        // Gabungkan brand dan nama
        String fullName = productName;
        if (brand != null && !brand.isEmpty() && productName != null && !productName.contains(brand)) {
            fullName = brand + " " + productName;
        }
        // Tambahkan quantity jika ada
        if (quantity != null && !quantity.isEmpty() && fullName != null && !fullName.contains(quantity)) {
            fullName = fullName + " " + quantity;
        }
        if (fullName == null || fullName.isEmpty()) {
            fullName = "Produk";
        }
        
        // Final variable untuk digunakan di lambda
        final String finalFullName = fullName;
        
        StringBuilder message = new StringBuilder("Nama: " + finalFullName);
        if (category != null && !category.isEmpty()) {
            message.append("\nKategori: ").append(category);
        }
        if (quantity != null && !quantity.isEmpty()) {
            message.append("\nUkuran: ").append(quantity);
        }
        message.append("\n\nNote: Harga perlu diinput manual");
        message.append("\n\nIngin tambah produk ini ke database?");
        
        new AlertDialog.Builder(this)
            .setTitle("Produk Ditemukan")
            .setMessage(message.toString())
            .setPositiveButton("Tambah", (dialog, which) -> {
                // Buka AddProductActivity dengan data dari API pre-filled
                Intent intent = new Intent(this, AddProductActivity.class);
                intent.putExtra("barcode", barcode);
                intent.putExtra("name", finalFullName);
                // Note: Open Food Facts tidak menyediakan harga
                startActivity(intent);
            })
            .setNegativeButton("Batal", null)
            .show();
    }

    private void showPaymentBottomSheet() {
        Double cartTotal = viewModel.getCartTotal().getValue();
        if (cartTotal == null || cartTotal <= 0) {
            Toast.makeText(this, "Keranjang kosong", Toast.LENGTH_SHORT).show();
            return;
        }
        
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        android.view.View view = getLayoutInflater().inflate(R.layout.layout_bottom_sheet_payment, null);
        dialog.setContentView(view);
        
        java.text.NumberFormat formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("id-ID"));
        
        android.widget.TextView tvPaymentTotal = view.findViewById(R.id.tvPaymentTotal);
        tvPaymentTotal.setText(formatter.format(cartTotal));
        
        // Metode pembayaran
        com.google.android.material.button.MaterialButtonToggleGroup togglePaymentMethod = view.findViewById(R.id.togglePaymentMethod);
        android.view.View layoutCashInput = view.findViewById(R.id.layoutCashInput);
        
        // Input uang bayar
        com.google.android.material.textfield.TextInputEditText etPaymentAmount = view.findViewById(R.id.etPaymentAmount);
        com.google.android.material.button.MaterialButton btnQuick5k = view.findViewById(R.id.btnQuick5k);
        com.google.android.material.button.MaterialButton btnQuick10k = view.findViewById(R.id.btnQuick10k);
        com.google.android.material.button.MaterialButton btnQuick100k = view.findViewById(R.id.btnQuick100k);
        
        // Display kembalian
        android.widget.TextView tvChange = view.findViewById(R.id.tvChange);
        com.google.android.material.card.MaterialCardView cardChange = view.findViewById(R.id.cardChange);
        com.google.android.material.card.MaterialCardView cardInsufficient = view.findViewById(R.id.cardInsufficient);
        android.widget.TextView tvShortage = view.findViewById(R.id.tvShortage);
        com.google.android.material.button.MaterialButton btnConfirm = view.findViewById(R.id.btnConfirmPayment);
        
        // Default: Tunai
        boolean[] isQRIS = {false};
        
        // Toggle metode pembayaran
        togglePaymentMethod.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnQRIS) {
                    isQRIS[0] = true;
                    layoutCashInput.setVisibility(android.view.View.GONE);
                    cardChange.setVisibility(android.view.View.GONE);
                    cardInsufficient.setVisibility(android.view.View.GONE);
                    btnConfirm.setEnabled(true); // QRIS langsung bisa konfirmasi
                } else {
                    isQRIS[0] = false;
                    layoutCashInput.setVisibility(android.view.View.VISIBLE);
                    btnConfirm.setEnabled(false);
                    etPaymentAmount.requestFocus();
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(etPaymentAmount, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                    }
                }
            }
        });
        
        // Tombol cepat
        btnQuick5k.setOnClickListener(v -> {
            etPaymentAmount.setText("5000");
            etPaymentAmount.setSelection(etPaymentAmount.getText().length());
        });
        
        btnQuick10k.setOnClickListener(v -> {
            etPaymentAmount.setText("10000");
            etPaymentAmount.setSelection(etPaymentAmount.getText().length());
        });
        
        btnQuick100k.setOnClickListener(v -> {
            etPaymentAmount.setText("100000");
            etPaymentAmount.setSelection(etPaymentAmount.getText().length());
        });
        
        // Real-time calculation untuk Tunai
        android.text.TextWatcher paymentWatcher = new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isQRIS[0]) return; // Skip jika QRIS
                
                String amountStr = s.toString().trim();
                if (amountStr.isEmpty()) {
                    cardChange.setVisibility(android.view.View.GONE);
                    cardInsufficient.setVisibility(android.view.View.GONE);
                    btnConfirm.setEnabled(false);
                    return;
                }
                
                try {
                    double paymentAmount = Double.parseDouble(amountStr);
                    double change = paymentAmount - cartTotal;
                    
                    if (change >= 0) {
                        cardChange.setVisibility(android.view.View.VISIBLE);
                        cardInsufficient.setVisibility(android.view.View.GONE);
                        tvChange.setText(formatter.format(change));
                        btnConfirm.setEnabled(true);
                    } else {
                        cardChange.setVisibility(android.view.View.GONE);
                        cardInsufficient.setVisibility(android.view.View.VISIBLE);
                        tvShortage.setText(formatter.format(Math.abs(change)));
                        btnConfirm.setEnabled(false);
                    }
                } catch (NumberFormatException e) {
                    cardChange.setVisibility(android.view.View.GONE);
                    cardInsufficient.setVisibility(android.view.View.GONE);
                    btnConfirm.setEnabled(false);
                }
            }
            
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        };
        
        etPaymentAmount.addTextChangedListener(paymentWatcher);
        etPaymentAmount.requestFocus();
        etPaymentAmount.post(() -> etPaymentAmount.selectAll());
        
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(etPaymentAmount, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        }
        
        btnConfirm.setOnClickListener(v -> {
            if (isQRIS[0]) {
                // QRIS: langsung checkout tanpa perlu input uang
                viewModel.checkout();
                dialog.dismiss();
                Toast.makeText(this, "Transaksi Berhasil! (QRIS)", Toast.LENGTH_SHORT).show();
            } else {
                // Tunai: perlu validasi uang bayar
                String amountStr = etPaymentAmount.getText().toString().trim();
                if (amountStr.isEmpty()) {
                    Toast.makeText(this, "Masukkan uang bayar", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                try {
                    double paymentAmount = Double.parseDouble(amountStr);
                    double change = paymentAmount - cartTotal;
                    
                    if (change < 0) {
                        Toast.makeText(this, "Uang bayar kurang!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    viewModel.checkout();
                    dialog.dismiss();
                    
                    String message = "Transaksi Berhasil!";
                    if (change > 0) {
                        message += "\nKembalian: " + formatter.format(change);
                    }
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Input tidak valid", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        dialog.show();
    }

    // Permission handling sekarang menggunakan ActivityResultLauncher, tidak perlu onRequestPermissionsResult lagi
}
