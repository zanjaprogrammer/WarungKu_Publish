package com.alkahfprogrammer.warungku;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.appbar.MaterialToolbar;
import com.alkahfprogrammer.warungku.data.DataRepository;
import com.alkahfprogrammer.warungku.data.AppDatabase;
import com.alkahfprogrammer.warungku.viewmodel.AppViewModel;
import com.alkahfprogrammer.warungku.utils.DatabaseBackupUtils;
import com.alkahfprogrammer.warungku.utils.DatabaseRestoreUtils;
import com.alkahfprogrammer.warungku.utils.CurrencyFormatter;
import android.content.Intent;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.io.File;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;

public class SummaryActivity extends AppCompatActivity {

    private TextView tvNetProfit, tvInitialCapital, tvTotalIncome, tvTotalExpense, tvRoiPercentage, tvTotalStockPurchase, tvProgressPercentage;
    private TextView tvCartCount, tvCartTotal;
    private MaterialCardView cardNoCapitalWarning, cardBreakEvenAnnouncement, cardCartSummary, cardRoiSection, cardInitialCapital;
    private ProgressBar progressBarCapitalReturn;
    private DataRepository repository;
    private AppViewModel viewModel;
    private SharedPreferences prefs;
    private static final String PREF_NAME = "WarungKuPrefs";
    private static final String KEY_CAPITAL = "initial_capital";
    private static final String KEY_HIDE_PROGRESS_BAR = "hide_progress_bar";
    private static final String KEY_HIDE_CAPITAL_WARNING = "hide_capital_warning";

    private long currentStart, currentEnd;
    private LiveData<Double> incomeLive, expenseLive, profitLive, stockPurchaseLive;
    private LiveData<Double> totalIncomeLive, totalExpenseLive; // Untuk progress bar (total semua waktu)
    private int lastCheckedDay = -1;
    private ActivityResultLauncher<String> restoreFileLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_summary);

        repository = new DataRepository(getApplication());
        viewModel = AppViewModel.getInstance(getApplication());
        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        
        initViews();
        setupFilters();
        loadInitialCapital();
        setupCartObserver();
        updateProgressBarVisibility(); // Check visibility preference
        setupOfflineIndicator();

        // Default filter: Today
        updateTimeRange(RangeType.DAY);
        
        // Check if day has changed
        checkAndRefreshDailyData();
    }
    
    private void setupOfflineIndicator() {
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
    
    private void checkAndRefreshDailyData() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(java.util.TimeZone.getDefault());
        int currentDay = cal.get(Calendar.DAY_OF_YEAR);
        int currentYear = cal.get(Calendar.YEAR);
        
        // Check if day has changed (including year change)
        if (lastCheckedDay != currentDay || lastCheckedDay == -1) {
            lastCheckedDay = currentDay;
            // Refresh data untuk memastikan filter "Hari Ini" menggunakan tanggal yang benar
            ChipGroup chipGroup = findViewById(R.id.chipGroupFilter);
            int checkedId = chipGroup.getCheckedChipId();
            if (checkedId == R.id.chipDay) {
                updateTimeRange(RangeType.DAY);
            }
        }
    }

    private void initViews() {
        tvNetProfit = findViewById(R.id.tvNetProfit);
        tvInitialCapital = findViewById(R.id.tvInitialCapital);
        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvRoiPercentage = findViewById(R.id.tvRoiPercentage);
        tvTotalStockPurchase = findViewById(R.id.tvTotalStockPurchase);
        tvProgressPercentage = findViewById(R.id.tvProgressPercentage);
        cardNoCapitalWarning = findViewById(R.id.cardNoCapitalWarning);
        cardBreakEvenAnnouncement = findViewById(R.id.cardBreakEvenAnnouncement);
        cardCartSummary = findViewById(R.id.cardCartSummary);
        cardRoiSection = findViewById(R.id.cardRoiSection);
        cardInitialCapital = findViewById(R.id.cardInitialCapital);
        progressBarCapitalReturn = findViewById(R.id.progressBarCapitalReturn);
        tvCartCount = findViewById(R.id.tvCartCount);
        tvCartTotal = findViewById(R.id.tvCartTotal);
        
        // Setup tombol hide progress bar di announcement card
        android.view.View btnHideProgressBar = findViewById(R.id.btnHideProgressBar);
        if (btnHideProgressBar != null) {
            btnHideProgressBar.setOnClickListener(v -> {
                prefs.edit().putBoolean(KEY_HIDE_PROGRESS_BAR, true).apply();
                updateProgressBarVisibility();
                Toast.makeText(this, "Progress bar disembunyikan", Toast.LENGTH_SHORT).show();
            });
        }
        
        // Setup tombol skip/hide warning modal awal
        android.view.View btnSkipCapitalWarning = findViewById(R.id.btnSkipCapitalWarning);
        if (btnSkipCapitalWarning != null) {
            btnSkipCapitalWarning.setOnClickListener(v -> {
                prefs.edit().putBoolean(KEY_HIDE_CAPITAL_WARNING, true).apply();
                updateCapitalWarningVisibility();
                Toast.makeText(this, "Peringatan disembunyikan", Toast.LENGTH_SHORT).show();
            });
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_backup) {
                performBackup();
                return true;
            } else if (id == R.id.menu_restore) {
                showRestoreDialog();
                return true;
            }
            return false;
        });
        
        // Hide auth button (feature removed)
        com.google.android.material.button.MaterialButton btnAuth = findViewById(R.id.btnAuth);
        if (btnAuth != null) {
            btnAuth.setVisibility(View.GONE);
        }

        // Tombol laporan di card
        findViewById(R.id.cardReport).setOnClickListener(v -> {
            startActivity(new Intent(this, ReportActivity.class));
        });
        // Toolbar back button removed per user request

        findViewById(R.id.btnSetCapital).setOnClickListener(v -> showCapitalDialog());
        findViewById(R.id.btnSetCapitalFromWarning).setOnClickListener(v -> showCapitalDialog());
        findViewById(R.id.btnCheckout).setOnClickListener(v -> {
            showPaymentBottomSheet();
        });

        // Setup Backup & Restore
        setupBackupRestore();

        updateCapitalWarningVisibility();
        setupBottomNavigation();
    }
    
    private void setupBackupRestore() {
        // Setup file picker launcher untuk restore
        restoreFileLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    showRestoreConfirmationDialog(uri);
                }
            }
        );
        
        // Backup & Restore buttons (from card - kept for backward compatibility, but hidden)
        View btnBackup = findViewById(R.id.btnBackup);
        if (btnBackup != null) {
            btnBackup.setOnClickListener(v -> performBackup());
        }
        View btnRestore = findViewById(R.id.btnRestore);
        if (btnRestore != null) {
            btnRestore.setOnClickListener(v -> showRestoreDialog());
        }
        
        // Hide manage employees card (feature removed)
        MaterialCardView cardManageEmployees = findViewById(R.id.cardManageEmployees);
        if (cardManageEmployees != null) {
            cardManageEmployees.setVisibility(View.GONE);
        }
        
    }
    
    private void showRestoreDialog() {
        restoreFileLauncher.launch("application/octet-stream");
    }
    
    private void performBackup() {
        // Show loading
        Toast.makeText(this, "Membuat backup...", Toast.LENGTH_SHORT).show();
        
        // Run backup di background thread
        AppDatabase.databaseWriteExecutor.execute(() -> {
            File backupFile = DatabaseBackupUtils.backupDatabase(this);
            
            runOnUiThread(() -> {
                if (backupFile != null && backupFile.exists()) {
                    // Offer to share
                    Intent shareIntent = DatabaseBackupUtils.getShareIntent(this, backupFile);
                    if (shareIntent != null) {
                        startActivity(Intent.createChooser(shareIntent, "Bagikan Backup"));
                    }
                    Toast.makeText(this, 
                        "Backup berhasil!\nFile: " + backupFile.getName(), 
                        Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Gagal membuat backup", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
    
    private void showRestoreConfirmationDialog(android.net.Uri uri) {
        new AlertDialog.Builder(this)
            .setTitle("Konfirmasi Restore")
            .setMessage("Ini akan mengganti semua data dengan data dari backup.\n\n" +
                       "Backup otomatis akan dibuat sebelum restore.\n\n" +
                       "Lanjutkan?")
            .setPositiveButton("Ya, Restore", (dialog, which) -> {
                performRestore(uri);
            })
            .setNegativeButton("Batal", null)
            .show();
    }
    
    private void performRestore(android.net.Uri uri) {
        Toast.makeText(this, "Memulihkan data...", Toast.LENGTH_SHORT).show();
        
        // Run restore di background thread
        AppDatabase.databaseWriteExecutor.execute(() -> {
            boolean success = DatabaseRestoreUtils.restoreDatabaseFromUri(this, uri);
            
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, 
                        "Restore berhasil!\nAplikasi akan dimuat ulang.", 
                        Toast.LENGTH_LONG).show();
                    
                    // Restart activity untuk reload data
                    finish();
                    startActivity(new Intent(this, SummaryActivity.class));
                } else {
                    Toast.makeText(this, 
                        "Gagal restore. Pastikan file backup valid.", 
                        Toast.LENGTH_LONG).show();
                }
            });
        });
    }
    
    private void setupCartObserver() {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"));
        
        viewModel.getCartItems().observe(this, items -> {
            if (items != null && !items.isEmpty()) {
                cardCartSummary.setVisibility(View.VISIBLE);
                // Hitung total quantity (bukan jumlah tipe produk)
                int totalQty = 0;
                for (com.alkahfprogrammer.warungku.data.model.CartItem item : items) {
                    totalQty += item.quantity;
                }
                tvCartCount.setText(totalQty + " Barang");
            } else {
                cardCartSummary.setVisibility(View.GONE);
            }
        });

        viewModel.getCartTotal().observe(this, total -> {
            tvCartTotal.setText(formatter.format(total != null ? total : 0));
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_summary);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_sell) {
                startActivity(new Intent(this, SellActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_stock) {
                startActivity(new Intent(this, StockActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_money) {
                startActivity(new Intent(this, HistoryActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return id == R.id.nav_summary;
        });
    }

    private void setupFilters() {
        ChipGroup chipGroup = findViewById(R.id.chipGroupFilter);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty())
                return;
            int id = checkedIds.get(0);
            if (id == R.id.chipDay)
                updateTimeRange(RangeType.DAY);
            else if (id == R.id.chipWeek)
                updateTimeRange(RangeType.WEEK);
            else if (id == R.id.chipMonth)
                updateTimeRange(RangeType.MONTH);
            else if (id == R.id.chipYear)
                updateTimeRange(RangeType.YEAR);
        });
    }

    private enum RangeType {
        DAY, WEEK, MONTH, YEAR
    }

    private void updateTimeRange(RangeType type) {
        Calendar cal = Calendar.getInstance();
        // Gunakan timezone lokal untuk perhitungan yang akurat
        cal.setTimeZone(java.util.TimeZone.getDefault());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        currentEnd = System.currentTimeMillis();

        switch (type) {
            case DAY:
                // Pastikan menggunakan hari ini (bukan kemarin)
                currentStart = cal.getTimeInMillis();
                break;
            case WEEK:
                cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                currentStart = cal.getTimeInMillis();
                break;
            case MONTH:
                cal.set(Calendar.DAY_OF_MONTH, 1);
                currentStart = cal.getTimeInMillis();
                break;
            case YEAR:
                cal.set(Calendar.DAY_OF_YEAR, 1);
                currentStart = cal.getTimeInMillis();
                break;
        }

        observeData();
    }

    private void observeData() {
        // Remove old observers if any
        if (incomeLive != null) {
            incomeLive.removeObservers(this);
            expenseLive.removeObservers(this);
            profitLive.removeObservers(this);
            if (stockPurchaseLive != null) {
                stockPurchaseLive.removeObservers(this);
            }
        }
        
        // Remove total observers if any
        if (totalIncomeLive != null) {
            totalIncomeLive.removeObservers(this);
            totalExpenseLive.removeObservers(this);
        }

        // Data untuk periode yang dipilih (untuk display)
        incomeLive = repository.getIncomeInRange(currentStart, currentEnd);
        expenseLive = repository.getExpenseInRange(currentStart, currentEnd);
        profitLive = repository.getProfitInRange(currentStart, currentEnd);
        stockPurchaseLive = repository.getTotalStockPurchaseInRange(currentStart, currentEnd);

        // Data total semua waktu (untuk progress bar perkembangan modal)
        totalIncomeLive = repository.getTotalIncome();
        totalExpenseLive = repository.getTotalExpense();

        incomeLive.observe(this, this::updateCalculations);
        expenseLive.observe(this, this::updateCalculations);
        profitLive.observe(this, this::updateCalculations);
        stockPurchaseLive.observe(this, this::updateCalculations);
        
        // Observer untuk total (progress bar)
        totalIncomeLive.observe(this, this::updateProgressBar);
        totalExpenseLive.observe(this, this::updateProgressBar);
    }

    private void updateCalculations(Double dummy) {
        double income = incomeLive.getValue() != null ? incomeLive.getValue() : 0.0;
        double expense = expenseLive.getValue() != null ? expenseLive.getValue() : 0.0;
        double profit = profitLive.getValue() != null ? profitLive.getValue() : 0.0;
        double stockPurchase = stockPurchaseLive != null && stockPurchaseLive.getValue() != null ? stockPurchaseLive.getValue() : 0.0;

        double netProfit = income - expense;

        tvTotalIncome.setText(formatCurrency(income));
        tvTotalExpense.setText(formatCurrency(expense));
        tvNetProfit.setText(formatCurrency(netProfit));
        tvTotalStockPurchase.setText(formatCurrency(stockPurchase));

        // Perkembangan modal akan diupdate oleh updateProgressBar() yang menggunakan total semua waktu
    }
    
    /**
     * Update progress bar menggunakan total net profit dari semua waktu (tidak ter-filter)
     * Ini menunjukkan total progress pengembalian modal sejak awal
     */
    private void updateProgressBar(Double dummy) {
        double totalIncome = totalIncomeLive != null && totalIncomeLive.getValue() != null ? totalIncomeLive.getValue() : 0.0;
        double totalExpense = totalExpenseLive != null && totalExpenseLive.getValue() != null ? totalExpenseLive.getValue() : 0.0;
        double capital = getCapital();

        // Total net profit dari semua waktu (untuk progress bar)
        double totalNetProfit = totalIncome - totalExpense;

        // Update progress bar pengembalian modal
        double progressPercent = 0;
        if (capital > 0) {
            progressPercent = (totalNetProfit / capital) * 100;
            // Batasi maksimal 100%
            if (progressPercent > 100) {
                progressPercent = 100;
            }
            if (progressPercent < 0) {
                progressPercent = 0;
            }
            
            int progressInt = (int) progressPercent;
            progressBarCapitalReturn.setProgress(progressInt);
            tvProgressPercentage.setText(String.format(Locale.getDefault(), "%.1f%%", progressPercent));
            
            // Update teks perkembangan modal: sisa uang yang dibutuhkan untuk kembali modal
            if (progressPercent >= 100) {
                // Sudah kembali modal: hide angka, hanya tampilkan progress bar
                tvRoiPercentage.setVisibility(View.GONE);
                cardBreakEvenAnnouncement.setVisibility(View.VISIBLE);
            } else {
                // Belum kembali modal: tampilkan sisa uang yang dibutuhkan
                double remaining = capital - totalNetProfit;
                if (remaining > 0) {
                    tvRoiPercentage.setVisibility(View.VISIBLE);
                    tvRoiPercentage.setText(formatCurrency(remaining));
                    tvRoiPercentage.setTextColor(getResources().getColor(R.color.primary));
                } else {
                    // Jika minus (sudah lebih dari modal), tetap tampilkan 0
                    tvRoiPercentage.setVisibility(View.VISIBLE);
                    tvRoiPercentage.setText(formatCurrency(0));
                    tvRoiPercentage.setTextColor(getResources().getColor(R.color.primary));
                }
                cardBreakEvenAnnouncement.setVisibility(View.GONE);
            }
        } else {
            progressBarCapitalReturn.setProgress(0);
            tvProgressPercentage.setText("0%");
            tvRoiPercentage.setVisibility(View.VISIBLE);
            tvRoiPercentage.setText(formatCurrency(0));
            cardBreakEvenAnnouncement.setVisibility(View.GONE);
        }
        
        // Update visibility card modal awal berdasarkan progress (harus di luar if-else capital)
        if (cardInitialCapital != null) {
            if (capital > 0 && progressPercent >= 100) {
                // Hide card modal awal jika progress >= 100%
                cardInitialCapital.setVisibility(View.GONE);
            } else {
                // Show card modal awal jika progress < 100% atau modal belum diisi
                cardInitialCapital.setVisibility(View.VISIBLE);
            }
        }
        
        // Update visibility berdasarkan preference
        updateProgressBarVisibility();
    }
    
    /**
     * Update visibility progress bar berdasarkan preference
     * Juga hide announcement card jika progress bar di-hide
     */
    private void updateProgressBarVisibility() {
        boolean hideProgressBar = prefs.getBoolean(KEY_HIDE_PROGRESS_BAR, false);
        double capital = getCapital();
        double totalIncome = totalIncomeLive != null && totalIncomeLive.getValue() != null ? totalIncomeLive.getValue() : 0.0;
        double totalExpense = totalExpenseLive != null && totalExpenseLive.getValue() != null ? totalExpenseLive.getValue() : 0.0;
        double totalNetProfit = totalIncome - totalExpense;
        double progressPercent = capital > 0 ? (totalNetProfit / capital) * 100 : 0;
        if (progressPercent > 100) progressPercent = 100;
        if (progressPercent < 0) progressPercent = 0;
        
        if (hideProgressBar) {
            // Hide progress bar section
            if (cardRoiSection != null) {
                cardRoiSection.setVisibility(View.GONE);
            }
            // Hide announcement card juga
            if (cardBreakEvenAnnouncement != null) {
                cardBreakEvenAnnouncement.setVisibility(View.GONE);
            }
            // Hide card modal awal jika progress >= 100%
            if (progressPercent >= 100 && cardInitialCapital != null) {
                cardInitialCapital.setVisibility(View.GONE);
            }
        } else {
            // Show progress bar section
            if (cardRoiSection != null) {
                cardRoiSection.setVisibility(View.VISIBLE);
            }
            // Announcement card visibility akan diatur oleh updateProgressBar()
            // Card modal awal visibility: hide jika progress >= 100%, show jika < 100%
            if (cardInitialCapital != null) {
                if (progressPercent >= 100) {
                    cardInitialCapital.setVisibility(View.GONE);
                } else {
                    cardInitialCapital.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void loadInitialCapital() {
        tvInitialCapital.setText(formatCurrency(getCapital()));
        updateCapitalWarningVisibility();
    }

    private void updateCapitalWarningVisibility() {
        if (cardNoCapitalWarning != null) {
            boolean hideWarning = prefs.getBoolean(KEY_HIDE_CAPITAL_WARNING, false);
            double capital = getCapital();
            // Tampilkan warning hanya jika modal belum diisi DAN user belum memilih untuk hide warning
            if (capital <= 0 && !hideWarning) {
                cardNoCapitalWarning.setVisibility(View.VISIBLE);
            } else {
                cardNoCapitalWarning.setVisibility(View.GONE);
            }
        }
    }

    private double getCapital() {
        return (double) prefs.getFloat(KEY_CAPITAL, 0f);
    }

    private void showCapitalDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Modal Awal");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf((int) getCapital()));
        builder.setView(input);

        builder.setPositiveButton("Simpan", (dialog, which) -> {
            try {
                float capital = Float.parseFloat(input.getText().toString());
                prefs.edit().putFloat(KEY_CAPITAL, capital).apply();
                loadInitialCapital();
                updateCapitalWarningVisibility();
                updateCalculations(0.0);
            } catch (Exception e) {
                Toast.makeText(this, "Input tidak valid", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Batal", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private String formatCurrency(double amount) {
        return CurrencyFormatter.format(amount);
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
        
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"));
        
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
                    layoutCashInput.setVisibility(View.GONE);
                    cardChange.setVisibility(View.GONE);
                    cardInsufficient.setVisibility(View.GONE);
                    btnConfirm.setEnabled(true); // QRIS langsung bisa konfirmasi
                } else {
                    isQRIS[0] = false;
                    layoutCashInput.setVisibility(View.VISIBLE);
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
                    cardChange.setVisibility(View.GONE);
                    cardInsufficient.setVisibility(View.GONE);
                    btnConfirm.setEnabled(false);
                    return;
                }
                
                try {
                    double paymentAmount = Double.parseDouble(amountStr);
                    double change = paymentAmount - cartTotal;
                    
                    if (change >= 0) {
                        cardChange.setVisibility(View.VISIBLE);
                        cardInsufficient.setVisibility(View.GONE);
                        tvChange.setText(formatter.format(change));
                        btnConfirm.setEnabled(true);
                    } else {
                        cardChange.setVisibility(View.GONE);
                        cardInsufficient.setVisibility(View.VISIBLE);
                        tvShortage.setText(formatter.format(Math.abs(change)));
                        btnConfirm.setEnabled(false);
                    }
                } catch (NumberFormatException e) {
                    cardChange.setVisibility(View.GONE);
                    cardInsufficient.setVisibility(View.GONE);
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
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Check if day has changed and refresh data
        checkAndRefreshDailyData();
        
        // Check network status
        setupOfflineIndicator();
    }
}
