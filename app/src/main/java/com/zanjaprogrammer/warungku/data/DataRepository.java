package com.zanjaprogrammer.warungku.data;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.zanjaprogrammer.warungku.data.dao.CashFlowDao;
import com.zanjaprogrammer.warungku.data.dao.ProductDao;
import com.zanjaprogrammer.warungku.data.entity.CashFlow;
import com.zanjaprogrammer.warungku.data.entity.Product;

import java.util.List;

public class DataRepository {
    private final ProductDao productDao;
    private final CashFlowDao cashFlowDao;
    private final LiveData<List<Product>> allProducts;
    private final LiveData<List<CashFlow>> allHistory;
    private final Application application;

    public DataRepository(Application application) {
        this.application = application;
        AppDatabase db = AppDatabase.getDatabase(application);
        productDao = db.productDao();
        cashFlowDao = db.cashFlowDao();
        allProducts = productDao.getAllProducts();
        allHistory = cashFlowDao.getAllHistory();
    }

    public LiveData<List<Product>> getAllProducts() {
        return allProducts;
    }

    public LiveData<List<CashFlow>> getAllHistory() {
        return allHistory;
    }

    public LiveData<Double> getCurrentBalance() {
        return cashFlowDao.getCurrentBalance();
    }

    public LiveData<Double> getTotalIncome() {
        return cashFlowDao.getTotalIncome();
    }

    public LiveData<Double> getTotalExpense() {
        return cashFlowDao.getTotalExpense();
    }

    public LiveData<Double> getIncomeInRange(long start, long end) {
        return cashFlowDao.getIncomeInRange(start, end);
    }

    public LiveData<Double> getExpenseInRange(long start, long end) {
        return cashFlowDao.getExpenseInRange(start, end);
    }

    public LiveData<Double> getProfitInRange(long start, long end) {
        return cashFlowDao.getProfitInRange(start, end);
    }

    public LiveData<Double> getTotalStockPurchase() {
        return cashFlowDao.getTotalStockPurchase();
    }

    public LiveData<Double> getTotalStockPurchaseInRange(long start, long end) {
        return cashFlowDao.getTotalStockPurchaseInRange(start, end);
    }

    public LiveData<List<Product>> getShoppingList() {
        return productDao.getShoppingList();
    }

    // Methods untuk laporan
    public LiveData<List<Product>> getTopSellingProducts(int limit) {
        return productDao.getTopSellingProducts(limit);
    }

    public LiveData<List<Product>> getUnsoldProducts() {
        return productDao.getUnsoldProducts();
    }

    public LiveData<List<CashFlow>> getCashFlowInRange(long start, long end) {
        return cashFlowDao.getCashFlowInRange(start, end);
    }

    public void insertProduct(Product product) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long productId = productDao.insert(product);
            
            // Catat biaya pembelian jika ada stok awal dan buyPrice
            if (product.currentStock > 0 && product.buyPrice != null) {
                double cost = product.buyPrice * product.currentStock;
                if (cost > 0) {
                    CashFlow flow = new CashFlow("OUT", cost, "Tambah Stok: " + product.name + " (" + product.currentStock + ")",
                            System.currentTimeMillis(), (int) productId, 0.0);
                    cashFlowDao.insert(flow);
                }
            }
            
            // Check stock notifications
            checkStockNotifications();
        });
    }

    public void updateProduct(Product product) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            productDao.update(product);
            // Check stock notifications
            checkStockNotifications();
        });
    }
    
    public void updateCashFlow(CashFlow cashFlow) {
        AppDatabase.databaseWriteExecutor.execute(() -> cashFlowDao.update(cashFlow));
    }
    
    public void insertCashFlow(CashFlow cashFlow) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            cashFlowDao.insert(cashFlow);
        });
    }

    public void sellProduct(Product product, int quantity) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            product.currentStock -= quantity;
            product.salesCount += quantity;
            product.lastSoldTimestamp = System.currentTimeMillis();
            productDao.update(product);

            double amount = product.sellPrice * quantity;
            Double profit = (product.buyPrice != null) ? (product.sellPrice - product.buyPrice) * quantity : 0.0;

            CashFlow flow = new CashFlow("IN", amount, "Jual " + product.name + " (" + quantity + ")",
                    System.currentTimeMillis(), product.id, profit);
            cashFlowDao.insert(flow);
            // Check stock notifications
            checkStockNotifications();
        });
    }

    public void addProductStock(Product product, int quantity, double buyPrice) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            product.currentStock += quantity;
            // Update buyPrice jika berbeda dari yang lama
            if (product.buyPrice == null || product.buyPrice != buyPrice) {
                product.buyPrice = buyPrice;
            }
            productDao.update(product);

            double cost = buyPrice * quantity;
            if (cost > 0) {
                CashFlow flow = new CashFlow("OUT", cost, "Tambah Stok: " + product.name + " (" + quantity + ")",
                        System.currentTimeMillis(), product.id, 0.0);
                cashFlowDao.insert(flow);
            }
            // Check stock notifications (stok mungkin sudah kembali normal)
            checkStockNotifications();
        });
    }

    public void deleteProduct(Product product) {
        AppDatabase.databaseWriteExecutor.execute(() -> productDao.delete(product));
    }
    
    /**
     * Check stock and send notifications if needed
     */
    private void checkStockNotifications() {
        // Run in background thread to avoid blocking
        AppDatabase.databaseWriteExecutor.execute(() -> {
            com.zanjaprogrammer.warungku.utils.StockNotificationHelper notificationHelper = 
                new com.zanjaprogrammer.warungku.utils.StockNotificationHelper(application);
            notificationHelper.checkAndNotify();
        });
    }

    public void getProductByBarcode(String barcode, ProductCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Product product = productDao.getProductByBarcode(barcode);
            if (product != null) {
                callback.onProductFound(product);
            } else {
                callback.onProductNotFound();
            }
        });
    }
    
    public interface ProductCallback {
        void onProductFound(Product product);
        void onProductNotFound();
    }

    public void checkout(List<com.zanjaprogrammer.warungku.data.model.CartItem> items) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            StringBuilder desc = new StringBuilder("Jual: ");
            double totalAmount = 0;
            double totalProfit = 0;

            long currentTime = System.currentTimeMillis();
            for (com.zanjaprogrammer.warungku.data.model.CartItem item : items) {
                Product product = item.product;
                int quantity = item.quantity;

                // Update stock, sales count, and last sold timestamp
                product.currentStock -= quantity;
                product.salesCount += quantity;
                product.lastSoldTimestamp = currentTime;
                productDao.update(product);

                // Accumulate totals
                totalAmount += product.sellPrice * quantity;
                double profit = (product.buyPrice != null) ? (product.sellPrice - product.buyPrice) * quantity : 0.0;
                totalProfit += profit;

                desc.append(product.name).append(" (").append(quantity).append("), ");
            }

            // Remove trailing comma
            String finalDesc = desc.toString();
            if (finalDesc.endsWith(", ")) {
                finalDesc = finalDesc.substring(0, finalDesc.length() - 2);
            }

            CashFlow flow = new CashFlow("IN", totalAmount, finalDesc,
                    System.currentTimeMillis(), null, totalProfit);
            cashFlowDao.insert(flow);
            // Check stock notifications
            checkStockNotifications();
        });
    }
}
