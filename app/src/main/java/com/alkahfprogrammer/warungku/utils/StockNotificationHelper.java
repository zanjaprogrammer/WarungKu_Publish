package com.alkahfprogrammer.warungku.utils;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.alkahfprogrammer.warungku.R;
import com.alkahfprogrammer.warungku.StockActivity;
import com.alkahfprogrammer.warungku.data.AppDatabase;
import com.alkahfprogrammer.warungku.data.dao.ProductDao;
import com.alkahfprogrammer.warungku.data.entity.Product;

import java.util.List;

/**
 * Helper class untuk handle stock notifications
 */
public class StockNotificationHelper {
    private static final String TAG = "StockNotificationHelper";
    private static final String CHANNEL_ID = "stock_notifications";
    private static final String CHANNEL_NAME = "Notifikasi Stok";
    private static final String PREF_NAME = "StockNotifications";
    private static final String KEY_LAST_NOTIFICATION_TIME = "last_notification_time";
    private static final long NOTIFICATION_COOLDOWN = 3600000; // 1 jam dalam milliseconds
    
    private final Context context;
    private final ProductDao productDao;
    private final SharedPreferences prefs;
    
    public StockNotificationHelper(Context context) {
        this.context = context.getApplicationContext();
        AppDatabase db = AppDatabase.getDatabase(this.context);
        this.productDao = db.productDao();
        this.prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        createNotificationChannel();
    }
    
    /**
     * Create notification channel untuk Android 8.0+
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifikasi untuk stok barang yang hampir habis atau habis");
            channel.enableVibration(true);
            channel.setShowBadge(true);
            
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    /**
     * Check stok dan kirim notifikasi jika perlu
     */
    public void checkAndNotify() {
        // Check permission untuk Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            if (!notificationManager.areNotificationsEnabled()) {
                Log.d(TAG, "Notifications not enabled by user");
                return;
            }
        }
        
        // Check cooldown (jangan spam notification)
        long lastNotificationTime = prefs.getLong(KEY_LAST_NOTIFICATION_TIME, 0);
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastNotificationTime < NOTIFICATION_COOLDOWN) {
            Log.d(TAG, "Notification cooldown active, skipping");
            return;
        }
        
        // Check stok di background thread
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<Product> allProducts = productDao.getAllProductsSync();
                checkLowStock(allProducts);
                checkOutOfStock(allProducts);
            } catch (Exception e) {
                Log.e(TAG, "Error checking stock", e);
            }
        });
    }
    
    /**
     * Check produk dengan stok rendah (currentStock <= minStock)
     */
    private void checkLowStock(List<Product> products) {
        int lowStockCount = 0;
        StringBuilder productNames = new StringBuilder();
        
        for (Product product : products) {
            if (product.currentStock > 0 && product.currentStock <= product.minStock) {
                lowStockCount++;
                if (productNames.length() > 0) {
                    productNames.append(", ");
                }
                if (productNames.length() < 100) { // Limit panjang text
                    productNames.append(product.name);
                } else {
                    productNames.append("...");
                    break;
                }
            }
        }
        
        if (lowStockCount > 0) {
            String title = lowStockCount == 1 ? "Stok Hampir Habis" : lowStockCount + " Produk Stok Hampir Habis";
            String message = lowStockCount == 1 
                ? productNames.toString() + " stok hampir habis (" + getLowStockInfo(products) + ")"
                : lowStockCount + " produk perlu restock segera";
            
            sendNotification(1, title, message);
            updateLastNotificationTime();
        }
    }
    
    /**
     * Check produk yang stok habis (currentStock == 0)
     */
    private void checkOutOfStock(List<Product> products) {
        int outOfStockCount = 0;
        StringBuilder productNames = new StringBuilder();
        
        for (Product product : products) {
            if (product.currentStock == 0) {
                outOfStockCount++;
                if (productNames.length() > 0) {
                    productNames.append(", ");
                }
                if (productNames.length() < 100) { // Limit panjang text
                    productNames.append(product.name);
                } else {
                    productNames.append("...");
                    break;
                }
            }
        }
        
        if (outOfStockCount > 0) {
            String title = outOfStockCount == 1 ? "Stok Habis" : outOfStockCount + " Produk Stok Habis";
            String message = outOfStockCount == 1 
                ? productNames.toString() + " stok habis"
                : outOfStockCount + " produk stok habis, perlu restock";
            
            sendNotification(2, title, message);
            updateLastNotificationTime();
        }
    }
    
    /**
     * Get info stok untuk produk dengan stok rendah
     */
    private String getLowStockInfo(List<Product> products) {
        for (Product product : products) {
            if (product.currentStock > 0 && product.currentStock <= product.minStock) {
                return product.currentStock + "/" + product.minStock;
            }
        }
        return "";
    }
    
    /**
     * Send notification
     */
    private void sendNotification(int notificationId, String title, String message) {
        Intent intent = new Intent(context, StockActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stock)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER);
        
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "Notification sent: " + title);
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to send notification", e);
        }
    }
    
    /**
     * Update last notification time untuk cooldown
     */
    private void updateLastNotificationTime() {
        prefs.edit().putLong(KEY_LAST_NOTIFICATION_TIME, System.currentTimeMillis()).apply();
    }
    
    /**
     * Clear all stock notifications
     */
    public void clearNotifications() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(1); // Low stock notification
        notificationManager.cancel(2); // Out of stock notification
    }
}

