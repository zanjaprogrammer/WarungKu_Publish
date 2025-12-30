package com.alkahfprogrammer.warungku;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.appbar.MaterialToolbar;
import com.alkahfprogrammer.warungku.adapters.ProductReportAdapter;
import com.alkahfprogrammer.warungku.data.entity.CashFlow;
import com.alkahfprogrammer.warungku.data.entity.Product;
import com.alkahfprogrammer.warungku.viewmodel.AppViewModel;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {

    private AppViewModel viewModel;
    private TextView tvIncome, tvExpense, tvProfit;
    private LineChart chartTrend;
    private RecyclerView rvTopSelling, rvUnsold;
    private ProductReportAdapter topSellingAdapter, unsoldAdapter;
    
    private long currentStart, currentEnd;
    private LiveData<List<CashFlow>> cashFlowLive;
    private LiveData<Double> incomeLive, expenseLive;
    private LiveData<List<Product>> topSellingLive, unsoldLive;

    private enum PeriodType {
        DAY, WEEK, MONTH, YEAR
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_report);

        viewModel = AppViewModel.getInstance(getApplication());
        
        initViews();
        setupFilters();
        setupRecyclerViews();
        setupBottomNavigation();
        
        // Default: Hari Ini
        updatePeriod(PeriodType.DAY);
    }

    private void initViews() {
        tvIncome = findViewById(R.id.tvIncome);
        tvExpense = findViewById(R.id.tvExpense);
        tvProfit = findViewById(R.id.tvProfit);
        chartTrend = findViewById(R.id.chartTrend);
        rvTopSelling = findViewById(R.id.rvTopSelling);
        rvUnsold = findViewById(R.id.rvUnsold);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        // Set navigation icon (back arrow)
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> {
            // Navigate to SummaryActivity
            Intent intent = new Intent(this, SummaryActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void setupFilters() {
        ChipGroup chipGroup = findViewById(R.id.chipGroupPeriod);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipDay) {
                updatePeriod(PeriodType.DAY);
            } else if (id == R.id.chipWeek) {
                updatePeriod(PeriodType.WEEK);
            } else if (id == R.id.chipMonth) {
                updatePeriod(PeriodType.MONTH);
            } else if (id == R.id.chipYear) {
                updatePeriod(PeriodType.YEAR);
            }
        });
    }

    private void updatePeriod(PeriodType type) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        currentEnd = System.currentTimeMillis();

        switch (type) {
            case DAY:
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
        // Remove old observers
        if (incomeLive != null) incomeLive.removeObservers(this);
        if (expenseLive != null) expenseLive.removeObservers(this);
        if (cashFlowLive != null) cashFlowLive.removeObservers(this);
        if (topSellingLive != null) topSellingLive.removeObservers(this);
        if (unsoldLive != null) unsoldLive.removeObservers(this);

        // Observe financial data
        incomeLive = viewModel.getIncomeInRange(currentStart, currentEnd);
        expenseLive = viewModel.getExpenseInRange(currentStart, currentEnd);
        // profitLive tidak digunakan lagi karena keuntungan = income - expense
        cashFlowLive = viewModel.getCashFlowInRange(currentStart, currentEnd);

        incomeLive.observe(this, income -> updateFinancialSummary());
        expenseLive.observe(this, expense -> updateFinancialSummary());
        // profitLive observer dihapus karena keuntungan dihitung dari income - expense
        cashFlowLive.observe(this, cashFlows -> updateChart(cashFlows));

        // Observe product data (tidak tergantung periode, selalu real-time)
        topSellingLive = viewModel.getTopSellingProducts(10);
        unsoldLive = viewModel.getUnsoldProducts();

        topSellingLive.observe(this, products -> {
            if (products != null) {
                topSellingAdapter.setProducts(products);
            }
        });

        unsoldLive.observe(this, products -> {
            if (products != null) {
                unsoldAdapter.setProducts(products);
            }
        });
    }

    private void updateFinancialSummary() {
        double income = incomeLive.getValue() != null ? incomeLive.getValue() : 0.0;
        double expense = expenseLive.getValue() != null ? expenseLive.getValue() : 0.0;
        // Keuntungan = Pendapatan - Pengeluaran (bukan dari profitLive yang hanya margin penjualan)
        double profit = income - expense;

        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"));
        tvIncome.setText(formatter.format(income));
        tvExpense.setText(formatter.format(expense));
        tvProfit.setText(formatter.format(profit));
    }

    private void updateChart(List<CashFlow> cashFlows) {
        if (cashFlows == null || cashFlows.isEmpty()) {
            chartTrend.clear();
            chartTrend.invalidate();
            return;
        }

        // Group by date
        Map<String, DailyData> dailyDataMap = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (CashFlow flow : cashFlows) {
            String date = dateFormat.format(new Date(flow.timestamp));
            DailyData data = dailyDataMap.getOrDefault(date, new DailyData(date));
            
            if ("IN".equals(flow.type)) {
                data.income += flow.amount;
            } else {
                data.expense += flow.amount;
            }
            
            dailyDataMap.put(date, data);
        }

        // Convert to entries
        List<DailyData> sortedData = new ArrayList<>(dailyDataMap.values());
        sortedData.sort((a, b) -> a.date.compareTo(b.date));

        List<Entry> incomeEntries = new ArrayList<>();
        List<Entry> expenseEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < sortedData.size(); i++) {
            DailyData data = sortedData.get(i);
            incomeEntries.add(new Entry(i, (float) data.income));
            expenseEntries.add(new Entry(i, (float) data.expense));
            
            // Format label: DD/MM
            try {
                Date date = dateFormat.parse(data.date);
                SimpleDateFormat labelFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
                labels.add(labelFormat.format(date));
            } catch (Exception e) {
                labels.add(data.date);
            }
        }

        // Create datasets
        LineDataSet incomeDataSet = new LineDataSet(incomeEntries, "Pendapatan");
        incomeDataSet.setColor(getColor(R.color.primary));
        incomeDataSet.setLineWidth(2f);
        incomeDataSet.setCircleColor(getColor(R.color.primary));
        incomeDataSet.setCircleRadius(4f);
        incomeDataSet.setValueTextColor(getColor(R.color.text_primary));
        incomeDataSet.setValueTextSize(10f);

        LineDataSet expenseDataSet = new LineDataSet(expenseEntries, "Pengeluaran");
        expenseDataSet.setColor(getColor(R.color.error));
        expenseDataSet.setLineWidth(2f);
        expenseDataSet.setCircleColor(getColor(R.color.error));
        expenseDataSet.setCircleRadius(4f);
        expenseDataSet.setValueTextColor(getColor(R.color.text_primary));
        expenseDataSet.setValueTextSize(10f);

        LineData lineData = new LineData(incomeDataSet, expenseDataSet);
        chartTrend.setData(lineData);

        // Configure chart
        chartTrend.getDescription().setEnabled(false);
        chartTrend.getLegend().setEnabled(true);
        chartTrend.getLegend().setTextColor(getColor(R.color.text_primary));
        
        XAxis xAxis = chartTrend.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(getColor(R.color.text_secondary));
        xAxis.setTextSize(10f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < labels.size()) {
                    return labels.get(index);
                }
                return "";
            }
        });

        chartTrend.getAxisLeft().setTextColor(getColor(R.color.text_secondary));
        chartTrend.getAxisRight().setEnabled(false);
        chartTrend.invalidate();
    }

    private void setupRecyclerViews() {
        topSellingAdapter = new ProductReportAdapter(true);
        rvTopSelling.setLayoutManager(new LinearLayoutManager(this));
        rvTopSelling.setAdapter(topSellingAdapter);

        unsoldAdapter = new ProductReportAdapter(false);
        rvUnsold.setLayoutManager(new LinearLayoutManager(this));
        rvUnsold.setAdapter(unsoldAdapter);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_summary);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new android.content.Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
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
            } else if (id == R.id.nav_money) {
                startActivity(new android.content.Intent(this, HistoryActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return id == R.id.nav_summary;
        });
    }

    // Helper class untuk data harian
    private static class DailyData {
        String date;
        double income = 0;
        double expense = 0;

        DailyData(String date) {
            this.date = date;
        }
    }
}

