package com.zanjaprogrammer.warungku;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.zanjaprogrammer.warungku.adapters.HistoryAdapter;
import com.zanjaprogrammer.warungku.databinding.ActivityHistoryBinding;
import com.zanjaprogrammer.warungku.viewmodel.AppViewModel;

public class HistoryActivity extends AppCompatActivity {

    private ActivityHistoryBinding binding;
    private AppViewModel viewModel;
    private HistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Use singleton instance untuk persist cart across activities
        viewModel = AppViewModel.getInstance(getApplication());
        setupRecyclerView();
        setupOfflineIndicator();

        viewModel.getAllHistory().observe(this, history -> {
            adapter.setItems(history);
        });

        // Format currency konsisten dengan MainActivity
        java.text.NumberFormat balanceFormatter = java.text.NumberFormat
                .getCurrencyInstance(java.util.Locale.forLanguageTag("id-ID"));
        
        viewModel.getBalance().observe(this, balance -> {
            binding.tvBalance.setText(balanceFormatter.format(balance != null ? balance : 0));
        });

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
            binding.tvCartTotal.setText(formatter.format(total != null ? total : 0));
        });

        binding.btnCheckout.setOnClickListener(v -> {
            showPaymentBottomSheet();
        });

        binding.bottomNavigation.setSelectedItemId(R.id.nav_money);
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
            } else if (id == R.id.nav_stock) {
                startActivity(new android.content.Intent(this, StockActivity.class));
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

        binding.btnRecordExpense.setOnClickListener(v -> showExpenseDialog());
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
                        android.widget.Toast.makeText(this, "Pengeluaran dicatat", android.widget.Toast.LENGTH_SHORT)
                                .show();
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.bottomNavigation.setSelectedItemId(R.id.nav_money);
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
        adapter = new HistoryAdapter();
        binding.rvHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.rvHistory.setAdapter(adapter);
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
