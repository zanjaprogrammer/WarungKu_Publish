package com.alkahfprogrammer.warungku;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.alkahfprogrammer.warungku.adapters.ProductStockAdapter;
import com.alkahfprogrammer.warungku.databinding.ActivityStockBinding;
import com.alkahfprogrammer.warungku.utils.ExcelExporter;
import com.alkahfprogrammer.warungku.utils.ExcelImporter;
import com.alkahfprogrammer.warungku.viewmodel.AppViewModel;

import java.io.InputStream;
import java.util.List;

public class StockActivity extends AppCompatActivity {

    private ActivityStockBinding binding;
    private AppViewModel viewModel;
    private ProductStockAdapter adapter;
    private List<com.alkahfprogrammer.warungku.data.entity.Product> allProducts;
    private ActivityResultLauncher<String> filePickerLauncher;
    private ActivityResultLauncher<String> fileSaverLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        binding = ActivityStockBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Use singleton instance untuk persist cart across activities
        viewModel = AppViewModel.getInstance(getApplication());
        
        setupRecyclerView();
        setupFilePickers();
        setupToolbarMenu();
        setupOfflineIndicator();

        viewModel.getAllProducts().observe(this, products -> {
            allProducts = products;
            adapter.setProducts(products);
        });

        // Observe Cart
        java.text.NumberFormat formatter = java.text.NumberFormat
                .getCurrencyInstance(java.util.Locale.forLanguageTag("id-ID"));
        viewModel.getCartItems().observe(this, items -> {
            if (items != null && !items.isEmpty()) {
                binding.cardCartSummary.setVisibility(android.view.View.VISIBLE);
                // Hitung total quantity (bukan jumlah tipe produk)
                int totalQty = 0;
                for (com.alkahfprogrammer.warungku.data.model.CartItem item : items) {
                    totalQty += item.quantity;
                }
                binding.tvCartCount.setText(totalQty + " Barang");
            } else {
                binding.cardCartSummary.setVisibility(android.view.View.GONE);
            }
        });

        viewModel.getCartTotal().observe(this, total -> {
            binding.tvCartTotal.setText(formatter.format(total != null ? total : 0));
        });

        binding.btnCheckout.setOnClickListener(v -> {
            showPaymentBottomSheet();
        });

        binding.fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddProductActivity.class);
            startActivity(intent);
        });

        binding.bottomNavigation.setSelectedItemId(R.id.nav_stock);
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                finish();
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_sell) {
                startActivity(new android.content.Intent(this, SellActivity.class));
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
        binding.bottomNavigation.setSelectedItemId(R.id.nav_stock);
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
        
        boolean isOnline = com.alkahfprogrammer.warungku.utils.NetworkUtils.isNetworkAvailable(this);
        
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
        adapter = new ProductStockAdapter(product -> {
            showStockActionDialog(product);
        });
        binding.rvStock.setLayoutManager(new LinearLayoutManager(this));
        binding.rvStock.setAdapter(adapter);
    }
    
    private void setupFilePickers() {
        // File picker for import
        filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    importProductsFromUri(uri);
                }
            }
        );
        
        // File saver for export
        fileSaverLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            uri -> {
                if (uri != null) {
                    exportProductsToUri(uri);
                }
            }
        );
    }
    
    private void setupToolbarMenu() {
        binding.toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_export) {
                exportProducts();
                return true;
            } else if (id == R.id.menu_import) {
                importProducts();
                return true;
            } else if (id == R.id.menu_template) {
                generateTemplate();
                return true;
            }
            return false;
        });
    }
    
    private void exportProducts() {
        if (allProducts == null || allProducts.isEmpty()) {
            Toast.makeText(this, "Tidak ada produk untuk diexport", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show progress dialog
        AlertDialog progressDialog = new AlertDialog.Builder(this)
            .setMessage("Mengexport produk...")
            .setCancelable(false)
            .create();
        progressDialog.show();
        
        // Run export in background
        new Thread(() -> {
            ExcelExporter.ExportResult result = ExcelExporter.exportProducts(this, allProducts);
            
            runOnUiThread(() -> {
                progressDialog.dismiss();
                
                if (result.success) {
                    // Share file using Intent
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                    
                    // Use FileProvider for secure file sharing
                    android.net.Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".fileprovider",
                        new java.io.File(result.filePath)
                    );
                    
                    shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Export Produk WarungKu");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, "File export produk dari aplikasi WarungKu");
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    
                    try {
                        startActivity(Intent.createChooser(shareIntent, "Bagikan atau Buka File Excel"));
                        Toast.makeText(this, "File tersimpan di Downloads", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        // Fallback: show file path
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("Export Berhasil");
                        builder.setMessage("File tersimpan di:\n" + result.filePath);
                        builder.setPositiveButton("OK", null);
                        builder.show();
                    }
                } else {
                    Toast.makeText(this, "Export gagal: " + result.errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }
    
    private void exportProductsToUri(Uri uri) {
        if (allProducts == null || allProducts.isEmpty()) {
            Toast.makeText(this, "Tidak ada produk untuk diexport", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // For now, use the default export method
        // In future, can implement direct write to URI
        exportProducts();
    }
    
    private void importProducts() {
        filePickerLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }
    
    private void importProductsFromUri(Uri uri) {
        AlertDialog progressDialog = new AlertDialog.Builder(this)
            .setMessage("Mengimport produk...")
            .setCancelable(false)
            .create();
        progressDialog.show();
        
        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                ExcelImporter.ImportResult result = ExcelImporter.importProductsFromStream(this, inputStream, allProducts);
                inputStream.close();
                
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    showImportResult(result);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error membaca file: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    
    private void showImportResult(ExcelImporter.ImportResult result) {
        // Save imported products to database
        for (com.alkahfprogrammer.warungku.data.entity.Product product : result.importedProducts) {
            // Check if product has ID (existing product to update)
            if (product.id > 0) {
                viewModel.updateProduct(product);
            } else {
                // New product
                viewModel.addProduct(product);
            }
        }
        
        // Show result dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Import Selesai");
        
        StringBuilder message = new StringBuilder();
        message.append("✅ Berhasil: ").append(result.successCount).append(" produk\n");
        message.append("❌ Gagal: ").append(result.failCount).append(" produk\n\n");
        
        if (!result.errors.isEmpty()) {
            message.append("Detail error:\n");
            int maxErrors = Math.min(result.errors.size(), 10); // Show max 10 errors
            for (int i = 0; i < maxErrors; i++) {
                message.append("• ").append(result.errors.get(i)).append("\n");
            }
            if (result.errors.size() > 10) {
                message.append("... dan ").append(result.errors.size() - 10).append(" error lainnya");
            }
        }
        
        builder.setMessage(message.toString());
        builder.setPositiveButton("OK", null);
        builder.show();
        
        // Refresh product list
        viewModel.refreshProducts();
    }
    
    private void generateTemplate() {
        AlertDialog progressDialog = new AlertDialog.Builder(this)
            .setMessage("Membuat template...")
            .setCancelable(false)
            .create();
        progressDialog.show();
        
        new Thread(() -> {
            ExcelExporter.ExportResult result = ExcelExporter.generateTemplate(this);
            
            runOnUiThread(() -> {
                progressDialog.dismiss();
                
                if (result.success) {
                    // Share file using Intent
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                    
                    // Use FileProvider for secure file sharing
                    android.net.Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".fileprovider",
                        new java.io.File(result.filePath)
                    );
                    
                    shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Template Import Produk WarungKu");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, "Template untuk import produk ke aplikasi WarungKu");
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    
                    try {
                        startActivity(Intent.createChooser(shareIntent, "Bagikan atau Buka Template Excel"));
                        Toast.makeText(this, "File template tersimpan di Downloads", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        // Fallback: show file path
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("Template Berhasil Dibuat");
                        builder.setMessage("File template tersimpan di:\n" + result.filePath);
                        builder.setPositiveButton("OK", null);
                        builder.show();
                    }
                } else {
                    Toast.makeText(this, "Gagal membuat template: " + result.errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private void showStockActionDialog(com.alkahfprogrammer.warungku.data.entity.Product product) {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        android.view.View view = getLayoutInflater().inflate(R.layout.layout_bottom_sheet_stock_action, null);
        dialog.setContentView(view);

        android.widget.TextView tvProductName = view.findViewById(R.id.tvProductName);
        tvProductName.setText(product.name);

        view.findViewById(R.id.btnEdit).setOnClickListener(v -> {
            dialog.dismiss();
            editProduct(product);
        });

        view.findViewById(R.id.btnRestock).setOnClickListener(v -> {
            dialog.dismiss();
            showRestockBottomSheet(product);
        });

        view.findViewById(R.id.btnDelete).setOnClickListener(v -> {
            dialog.dismiss();
            showDeleteConfirmDialog(product);
        });

        dialog.show();
    }
    
    private void editProduct(com.alkahfprogrammer.warungku.data.entity.Product product) {
        Intent intent = new Intent(this, AddProductActivity.class);
        intent.putExtra("product_id", product.id);
        intent.putExtra("edit_mode", true);
        startActivity(intent);
    }

    private void showRestockBottomSheet(com.alkahfprogrammer.warungku.data.entity.Product product) {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        android.view.View view = getLayoutInflater().inflate(R.layout.layout_bottom_sheet_restock, null);
        dialog.setContentView(view);

        android.widget.TextView tvProductName = view.findViewById(R.id.tvRestockProductName);
        tvProductName.setText("Restock: " + product.name);

        com.google.android.material.textfield.TextInputEditText etQuantity = view.findViewById(R.id.etRestockQuantity);
        com.google.android.material.textfield.TextInputEditText etBuyPrice = view.findViewById(R.id.etRestockBuyPrice);
        com.google.android.material.textfield.TextInputLayout tilBuyPrice = view.findViewById(R.id.tilBuyPrice);
        android.widget.TextView tvTotalPrice = view.findViewById(R.id.tvTotalPrice);
        android.widget.RadioGroup rgPriceType = view.findViewById(R.id.rgPriceType);
        android.widget.RadioButton rbPerItem = view.findViewById(R.id.rbPerItem);
        android.widget.RadioButton rbTotal = view.findViewById(R.id.rbTotal);

        // Set default values
        etQuantity.setText("1");
        final boolean[] isPerItemMode = {true};

        // Update hint and calculate total
        final java.text.NumberFormat formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("id-ID"));
        
        final java.lang.Runnable updateTotalPrice = new java.lang.Runnable() {
            @Override
            public void run() {
                try {
                    String qtyStr = etQuantity.getText().toString().trim();
                    String priceStr = etBuyPrice.getText().toString().trim();
                    
                    if (qtyStr.isEmpty() || priceStr.isEmpty()) {
                        tvTotalPrice.setText("Total: Rp 0");
                        return;
                    }

                    int qty = Integer.parseInt(qtyStr);
                    double price = Double.parseDouble(priceStr);

                    if (qty <= 0 || price <= 0) {
                        tvTotalPrice.setText("Total: Rp 0");
                        return;
                    }

                    double total;
                    if (isPerItemMode[0]) {
                        total = price * qty;
                    } else {
                        total = price;
                    }

                    tvTotalPrice.setText("Total: " + formatter.format(total));
                } catch (Exception e) {
                    tvTotalPrice.setText("Total: Rp 0");
                }
            }
        };
        
        android.text.TextWatcher priceWatcher = new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateTotalPrice.run();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        };

        android.text.TextWatcher qtyWatcher = new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateTotalPrice.run();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        };

        etQuantity.addTextChangedListener(qtyWatcher);
        etBuyPrice.addTextChangedListener(priceWatcher);

        rgPriceType.setOnCheckedChangeListener(new android.widget.RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.RadioGroup group, int checkedId) {
                if (checkedId == R.id.rbPerItem) {
                    isPerItemMode[0] = true;
                    tilBuyPrice.setHint("Harga Beli Per Barang (Rp)");
                    if (product.buyPrice != null) {
                        etBuyPrice.setText(String.valueOf((int) product.buyPrice.doubleValue()));
                    } else {
                        etBuyPrice.setText("");
                    }
                } else if (checkedId == R.id.rbTotal) {
                    isPerItemMode[0] = false;
                    tilBuyPrice.setHint("Total Harga Beli (Rp)");
                    etBuyPrice.setText("");
                }
                updateTotalPrice.run();
            }
        });

        // Initialize with per item mode
        if (product.buyPrice != null) {
            etBuyPrice.setText(String.valueOf((int) product.buyPrice.doubleValue()));
        }
        updateTotalPrice.run();

        view.findViewById(R.id.btnRestockSave).setOnClickListener(v -> {
            String qtyStr = etQuantity.getText().toString().trim();
            String buyPriceStr = etBuyPrice.getText().toString().trim();

            if (qtyStr.isEmpty() || buyPriceStr.isEmpty()) {
                android.widget.Toast.makeText(this, "Mohon isi jumlah dan harga", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int quantity = Integer.parseInt(qtyStr);
                double inputPrice = Double.parseDouble(buyPriceStr);

                if (quantity <= 0) {
                    android.widget.Toast.makeText(this, "Jumlah harus lebih dari 0", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }

                if (inputPrice <= 0) {
                    android.widget.Toast.makeText(this, "Harga harus lebih dari 0", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }

                // Calculate harga per unit berdasarkan mode
                double buyPricePerUnit;
                if (isPerItemMode[0]) {
                    buyPricePerUnit = inputPrice;
                } else {
                    buyPricePerUnit = inputPrice / quantity;
                }

                viewModel.addProductStock(product, quantity, buyPricePerUnit);
                android.widget.Toast.makeText(this, "Stok berhasil ditambahkan", android.widget.Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } catch (NumberFormatException e) {
                android.widget.Toast.makeText(this, "Input tidak valid", android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showDeleteConfirmDialog(com.alkahfprogrammer.warungku.data.entity.Product product) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Hapus Barang")
                .setMessage("Apakah Anda yakin ingin menghapus " + product.name + "?")
                .setPositiveButton("Hapus", (dialog, which) -> {
                    viewModel.deleteProduct(product);
                    android.widget.Toast.makeText(this, "Barang berhasil dihapus", android.widget.Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    
    private void showPaymentBottomSheet() {
        Double cartTotal = viewModel.getCartTotal().getValue();
        if (cartTotal == null || cartTotal <= 0) {
            android.widget.Toast.makeText(this, "Keranjang kosong", android.widget.Toast.LENGTH_SHORT).show();
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
                android.widget.Toast.makeText(this, "Transaksi Berhasil! (QRIS)", android.widget.Toast.LENGTH_SHORT).show();
            } else {
                // Tunai: perlu validasi uang bayar
                String amountStr = etPaymentAmount.getText().toString().trim();
                if (amountStr.isEmpty()) {
                    android.widget.Toast.makeText(this, "Masukkan uang bayar", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                
                try {
                    double paymentAmount = Double.parseDouble(amountStr);
                    double change = paymentAmount - cartTotal;
                    
                    if (change < 0) {
                        android.widget.Toast.makeText(this, "Uang bayar kurang!", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    viewModel.checkout();
                    dialog.dismiss();
                    
                    String message = "Transaksi Berhasil!";
                    if (change > 0) {
                        message += "\nKembalian: " + formatter.format(change);
                    }
                    android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show();
                } catch (NumberFormatException e) {
                    android.widget.Toast.makeText(this, "Input tidak valid", android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        dialog.show();
    }
}
