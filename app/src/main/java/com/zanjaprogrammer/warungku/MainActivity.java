package com.zanjaprogrammer.warungku;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.zanjaprogrammer.warungku.databinding.ActivityMainBinding;
import com.zanjaprogrammer.warungku.viewmodel.AppViewModel;
import com.zanjaprogrammer.warungku.utils.CurrencyFormatter;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private AppViewModel viewModel;
    private final NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"));
    private androidx.lifecycle.LiveData<Double> dailyIncomeLive, dailyExpenseLive;
    private int lastCheckedDay = -1;
    private androidx.activity.result.ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // FOR TESTING: Force show onboarding (uncomment untuk testing)
        // OnboardingActivity.resetOnboarding(this);

        // Check if first launch - show onboarding
        if (OnboardingActivity.isFirstLaunch(this)) {
            Intent intent = new Intent(this, OnboardingActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Use singleton instance untuk persist cart across activities
        viewModel = AppViewModel.getInstance(getApplication());

        observeData();
        setupListeners();
        checkAndRefreshDailyData();
        setupOfflineIndicator();
        setupNotificationPermission();
        requestNotificationPermission();
    }
    
    private void setupNotificationPermission() {
        requestPermissionLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
            isGranted -> {
                // Permission result handled silently
            }
        );
    }
    
    private void requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, 
                    android.Manifest.permission.POST_NOTIFICATIONS) != 
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                if (requestPermissionLauncher != null) {
                    requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
                }
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Check network status setiap kali resume
        setupOfflineIndicator();
        
        // Ensure home icon is selected when returning to MainActivity
        // Use post() to ensure the view is fully laid out
        if (binding != null) {
            binding.bottomNavigation.post(() -> {
                binding.bottomNavigation.setSelectedItemId(R.id.nav_home);
            });
        }
        
        // Check if day has changed and refresh data
        checkAndRefreshDailyData();
        
        // Force refresh daily data to ensure accuracy after transactions
        updateDailyDataObservers();
        
        // Check stock and send notifications if needed
        com.zanjaprogrammer.warungku.utils.StockNotificationHelper notificationHelper = 
            new com.zanjaprogrammer.warungku.utils.StockNotificationHelper(this);
        notificationHelper.checkAndNotify();
    }
    
    private void setupOfflineIndicator() {
        android.content.SharedPreferences prefs = getSharedPreferences("WarungKuPrefs", MODE_PRIVATE);
        boolean hideOfflineWarning = prefs.getBoolean("hide_offline_warning", false);
        
        if (hideOfflineWarning) {
            return; // User sudah memilih untuk menyembunyikan
        }
        
        // Find the include view
        android.view.View includeView = findViewById(R.id.offlineIndicator);
        if (includeView == null) {
            return;
        }
        
        // The include view IS the MaterialCardView, so we can cast it directly
        com.google.android.material.card.MaterialCardView cardOffline = (com.google.android.material.card.MaterialCardView) includeView;
        
        // Check network status
        boolean isOnline = com.zanjaprogrammer.warungku.utils.NetworkUtils.isNetworkAvailable(this);
        
        if (!isOnline) {
            cardOffline.setVisibility(android.view.View.VISIBLE);
            
            // Close button - find it within the card
            android.view.View btnClose = cardOffline.findViewById(R.id.btnCloseOfflineIndicator);
            if (btnClose != null) {
                btnClose.setOnClickListener(v -> {
                    cardOffline.setVisibility(android.view.View.GONE);
                    // Save preference untuk tidak tampil lagi
                    prefs.edit().putBoolean("hide_offline_warning", true).apply();
                });
            }
        } else {
            cardOffline.setVisibility(android.view.View.GONE);
        }
    }

    private void observeData() {
        // Observe daily data (hari ini saja)
        updateDailyDataObservers();

        viewModel.getBalance().observe(this, balance -> {
            binding.tvBalance.setText(formatter.format(balance != null ? balance : 0));
        });

        viewModel.getBalance().observe(this, balance -> {
            binding.tvBalance.setText(formatter.format(balance != null ? balance : 0));
        });

        viewModel.getShoppingList().observe(this, list -> {
            binding.tvLowStock.setText(String.valueOf(list != null ? list.size() : 0));
        });

        // Observe Cart (Home)
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
            binding.tvCartTotal.setText(formatter.format(total != null ? total : 0));
        });
    }

    private void setupListeners() {
        binding.bottomNavigation.setSelectedItemId(R.id.nav_home);
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_sell) {
                startActivity(new android.content.Intent(this, SellActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_stock) {
                startActivity(new android.content.Intent(this, StockActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_money) {
                startActivity(new android.content.Intent(this, HistoryActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_summary) {
                startActivity(new android.content.Intent(this, SummaryActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return true;
        });

        binding.btnSellLarge.setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, SellActivity.class));
            overridePendingTransition(0, 0);
        });

        binding.btnAddStockFast.setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, StockActivity.class));
            overridePendingTransition(0, 0);
        });

        binding.btnExpenseFast.setOnClickListener(v -> {
            showExpenseDialog();
        });

        binding.cardStockSummary.setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, ShoppingListActivity.class));
            overridePendingTransition(0, 0);
        });

        binding.btnCheckout.setOnClickListener(v -> {
            showPaymentBottomSheet();
        });
    }

    
    private void checkAndRefreshDailyData() {
        Calendar cal = Calendar.getInstance();
        int currentDay = cal.get(Calendar.DAY_OF_YEAR);
        int currentYear = cal.get(Calendar.YEAR);
        
        // Check if day has changed (including year change)
        if (lastCheckedDay != currentDay || lastCheckedDay == -1) {
            lastCheckedDay = currentDay;
            // Refresh daily data observers
            updateDailyDataObservers();
        }
    }
    
    private void updateDailyDataObservers() {
        // Remove old observers
        if (dailyIncomeLive != null) {
            dailyIncomeLive.removeObservers(this);
        }
        if (dailyExpenseLive != null) {
            dailyExpenseLive.removeObservers(this);
        }
        
        // Calculate today's range (00:00:00 to now)
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long todayStart = cal.getTimeInMillis();
        long todayEnd = System.currentTimeMillis();
        
        // Observe daily income and expense
        dailyIncomeLive = viewModel.getIncomeInRange(todayStart, todayEnd);
        dailyExpenseLive = viewModel.getExpenseInRange(todayStart, todayEnd);
        
        dailyIncomeLive.observe(this, income -> {
            binding.tvIncome.setText(CurrencyFormatter.format(income != null ? income : 0));
        });

        dailyExpenseLive.observe(this, expense -> {
            binding.tvExpense.setText(CurrencyFormatter.format(expense != null ? expense : 0));
        });
    }
    
    // Method to force refresh daily data after transactions
    public void refreshDailyData() {
        updateDailyDataObservers();
    }

    private void showExpenseDialog() {
        android.widget.EditText etDesc = new android.widget.EditText(this);
        etDesc.setHint("Kebutuhan (misal: Listrik)");
        android.widget.EditText etPrice = new android.widget.EditText(this);
        etPrice.setHint("Jumlah (Rp)");
        etPrice.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);
        layout.addView(etDesc);
        layout.addView(etPrice);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Catat Uang Keluar")
                .setView(layout)
                .setPositiveButton("Simpan", (dialog, which) -> {
                    String desc = etDesc.getText().toString().trim();
                    String sPrice = etPrice.getText().toString().trim();
                    if (!desc.isEmpty() && !sPrice.isEmpty()) {
                        double price = Double.parseDouble(sPrice);
                        com.zanjaprogrammer.warungku.data.entity.CashFlow flow = new com.zanjaprogrammer.warungku.data.entity.CashFlow(
                                "OUT", price, desc,
                                System.currentTimeMillis(), null, 0.0);
                        viewModel.insertCashFlow(flow);
                        
                        // Refresh daily data after recording expense
                        refreshDailyData();
                        
                        android.widget.Toast.makeText(this, "Pengeluaran dicatat", android.widget.Toast.LENGTH_SHORT)
                                .show();
                    }
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
        com.google.android.material.button.MaterialButton btnCash = view.findViewById(R.id.btnCash);
        com.google.android.material.button.MaterialButton btnQRIS = view.findViewById(R.id.btnQRIS);
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
                
                // Refresh daily data after successful checkout
                refreshDailyData();
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
                    
                    // Refresh daily data after successful checkout
                    refreshDailyData();
                    
                    String message = "Transaksi Berhasil!";
                    if (change > 0) {
                        message += "\nKembalian: " + CurrencyFormatter.format(change);
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
