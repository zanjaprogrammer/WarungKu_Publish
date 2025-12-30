package com.alkahfprogrammer.warungku.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.alkahfprogrammer.warungku.data.dao.CashFlowDao;
import com.alkahfprogrammer.warungku.data.dao.ProductDao;
import com.alkahfprogrammer.warungku.data.entity.CashFlow;
import com.alkahfprogrammer.warungku.data.entity.Product;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = { Product.class, CashFlow.class }, version = 7, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract ProductDao productDao();

    public abstract CashFlowDao cashFlowDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    // Migration from version 1 to 2: Add isFavorite and lastSoldTimestamp columns
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Add new columns with default values
            database.execSQL("ALTER TABLE products ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE products ADD COLUMN lastSoldTimestamp INTEGER NOT NULL DEFAULT 0");
        }
    };

    // Migration from version 2 to 3: Add barcode column
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Add barcode column (nullable TEXT)
            database.execSQL("ALTER TABLE products ADD COLUMN barcode TEXT");
        }
    };
    
    // Migration from version 3 to 4: Add sync fields
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Add sync fields to products
            database.execSQL("ALTER TABLE products ADD COLUMN synced INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE products ADD COLUMN lastSyncedAt INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE products ADD COLUMN cloudId TEXT");
            
            // Add sync fields to cash_flow
            database.execSQL("ALTER TABLE cash_flow ADD COLUMN synced INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE cash_flow ADD COLUMN lastSyncedAt INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE cash_flow ADD COLUMN cloudId TEXT");
        }
    };
    
    // Migration from version 4 to 5: Remove sync fields, add local_users table
    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Drop sync columns from products (SQLite doesn't support DROP COLUMN directly)
            // We'll create a new table without sync columns and copy data
            database.execSQL("CREATE TABLE products_new (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    "name TEXT NOT NULL," +
                    "sellPrice REAL NOT NULL," +
                    "buyPrice REAL," +
                    "currentStock INTEGER NOT NULL," +
                    "minStock INTEGER NOT NULL," +
                    "salesCount INTEGER NOT NULL DEFAULT 0," +
                    "isFavorite INTEGER NOT NULL DEFAULT 0," +
                    "lastSoldTimestamp INTEGER NOT NULL DEFAULT 0," +
                    "barcode TEXT" +
                    ")");
            database.execSQL("INSERT INTO products_new (id, name, sellPrice, buyPrice, currentStock, minStock, salesCount, isFavorite, lastSoldTimestamp, barcode) " +
                    "SELECT id, name, sellPrice, buyPrice, currentStock, minStock, salesCount, isFavorite, lastSoldTimestamp, barcode FROM products");
            database.execSQL("DROP TABLE products");
            database.execSQL("ALTER TABLE products_new RENAME TO products");
            
            // Drop sync columns from cash_flow
            database.execSQL("CREATE TABLE cash_flow_new (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    "type TEXT NOT NULL," +
                    "amount REAL NOT NULL," +
                    "description TEXT," +
                    "timestamp INTEGER NOT NULL," +
                    "productId INTEGER," +
                    "profit REAL" +
                    ")");
            database.execSQL("INSERT INTO cash_flow_new (id, type, amount, description, timestamp, productId, profit) " +
                    "SELECT id, type, amount, description, timestamp, productId, profit FROM cash_flow");
            database.execSQL("DROP TABLE cash_flow");
            database.execSQL("ALTER TABLE cash_flow_new RENAME TO cash_flow");
            
            // Create local_users table
            database.execSQL("CREATE TABLE local_users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    "email TEXT NOT NULL UNIQUE," +
                    "password TEXT NOT NULL," +
                    "name TEXT," +
                    "role TEXT NOT NULL," +
                    "createdAt INTEGER NOT NULL," +
                    "isActive INTEGER NOT NULL DEFAULT 1" +
                    ")");
        }
    };
    
    // Migration from version 5 to 6: Remove local_users table (no longer needed)
    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Drop local_users table (no longer needed)
            database.execSQL("DROP TABLE IF EXISTS local_users");
        }
    };
    
    // Migration from version 6 to 7: Add imageUrl column to products
    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Add imageUrl column (nullable TEXT)
            database.execSQL("ALTER TABLE products ADD COLUMN imageUrl TEXT");
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "warungku_db")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                            .fallbackToDestructiveMigration() // For development: drop and recreate if migration fails
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
