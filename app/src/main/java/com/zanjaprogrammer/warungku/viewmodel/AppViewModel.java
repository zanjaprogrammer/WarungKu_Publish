package com.zanjaprogrammer.warungku.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.zanjaprogrammer.warungku.data.DataRepository;
import com.zanjaprogrammer.warungku.data.entity.CashFlow;
import com.zanjaprogrammer.warungku.data.entity.Product;
import androidx.lifecycle.MutableLiveData;
import com.zanjaprogrammer.warungku.data.model.CartItem;
import java.util.ArrayList;
import java.util.List;

public class AppViewModel extends AndroidViewModel {
    private static AppViewModel instance;
    
    private final DataRepository repository;
    private final LiveData<List<Product>> allProducts;
    private final LiveData<List<CashFlow>> allHistory;

    private final MutableLiveData<List<CartItem>> cartItems = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Double> cartTotal = new MutableLiveData<>(0.0);

    public AppViewModel(Application application) {
        super(application);
        repository = new DataRepository(application);
        allProducts = repository.getAllProducts();
        allHistory = repository.getAllHistory();
        instance = this; // Set singleton instance
    }
    
    /**
     * Get singleton instance (untuk persist cart across activities)
     * Jika belum ada instance, buat baru dengan Application context
     */
    public static AppViewModel getInstance(Application application) {
        if (instance == null) {
            instance = new AppViewModel(application);
        }
        return instance;
    }

    public LiveData<List<CartItem>> getCartItems() {
        return cartItems;
    }

    public LiveData<Double> getCartTotal() {
        return cartTotal;
    }

    public void addToCart(Product product, int quantity) {
        if (product == null) {
            return; // Safety check
        }
        
        List<CartItem> current = cartItems.getValue();
        if (current == null)
            current = new ArrayList<>();

        boolean found = false;
        for (CartItem item : current) {
            if (item != null && item.product != null && item.product.id == product.id) {
                if (item.quantity + quantity <= product.currentStock) {
                    item.quantity += quantity;
                    found = true;
                } else {
                    // Logic handled by UI usually (show toast)
                }
                break;
            }
        }

        if (!found) {
            if (quantity > 0 && quantity <= product.currentStock) {
                current.add(new CartItem(product, quantity));
            }
        }

        cartItems.setValue(new ArrayList<>(current)); // Force notify
        calculateTotal();
    }

    public void removeFromCart(CartItem item) {
        List<CartItem> current = cartItems.getValue();
        if (current != null) {
            current.remove(item);
            cartItems.setValue(new ArrayList<>(current));
            calculateTotal();
        }
    }

    public void clearCart() {
        cartItems.setValue(new ArrayList<>());
        cartTotal.setValue(0.0);
    }

    private void calculateTotal() {
        double total = 0;
        List<CartItem> current = cartItems.getValue();
        if (current != null) {
            for (CartItem item : current) {
                if (item != null && item.product != null) {
                    total += item.getSubtotal();
                }
            }
        }
        cartTotal.setValue(total);
    }

    public void checkout() {
        List<CartItem> items = cartItems.getValue();
        if (items != null && !items.isEmpty()) {
            repository.checkout(items);
            clearCart();
        }
    }

    public LiveData<List<Product>> getAllProducts() {
        return allProducts;
    }

    public LiveData<List<CashFlow>> getAllHistory() {
        return allHistory;
    }

    public LiveData<Double> getBalance() {
        return repository.getCurrentBalance();
    }

    public LiveData<Double> getIncome() {
        return repository.getTotalIncome();
    }

    public LiveData<Double> getExpense() {
        return repository.getTotalExpense();
    }

    public LiveData<List<Product>> getShoppingList() {
        return repository.getShoppingList();
    }

    public void insertProduct(Product product) {
        repository.insertProduct(product);
    }
    
    public void addProduct(Product product) {
        repository.insertProduct(product);
    }

    public void updateProduct(Product product) {
        repository.updateProduct(product);
    }
    
    public void refreshProducts() {
        // Force refresh by re-observing (LiveData will automatically update)
        // This is a no-op but can be used to trigger refresh if needed
    }

    public void insertCashFlow(CashFlow cashFlow) {
        repository.insertCashFlow(cashFlow);
    }

    public void sellProduct(Product product, int quantity) {
        repository.sellProduct(product, quantity);
    }

    public void addProductStock(Product product, int quantity, double buyPrice) {
        repository.addProductStock(product, quantity, buyPrice);
    }

    public void deleteProduct(Product product) {
        repository.deleteProduct(product);
    }

    public void getProductByBarcode(String barcode, DataRepository.ProductCallback callback) {
        repository.getProductByBarcode(barcode, callback);
    }

    // Methods untuk laporan
    public LiveData<List<Product>> getTopSellingProducts(int limit) {
        return repository.getTopSellingProducts(limit);
    }

    public LiveData<List<Product>> getUnsoldProducts() {
        return repository.getUnsoldProducts();
    }

    public LiveData<List<CashFlow>> getCashFlowInRange(long start, long end) {
        return repository.getCashFlowInRange(start, end);
    }

    public LiveData<Double> getIncomeInRange(long start, long end) {
        return repository.getIncomeInRange(start, end);
    }

    public LiveData<Double> getExpenseInRange(long start, long end) {
        return repository.getExpenseInRange(start, end);
    }

    public LiveData<Double> getProfitInRange(long start, long end) {
        return repository.getProfitInRange(start, end);
    }
}
