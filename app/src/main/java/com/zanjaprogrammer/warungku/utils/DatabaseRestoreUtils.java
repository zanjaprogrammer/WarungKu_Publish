package com.zanjaprogrammer.warungku.utils;

import android.content.Context;
import android.net.Uri;
import com.zanjaprogrammer.warungku.data.AppDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;

public class DatabaseRestoreUtils {
    
    private static final String DB_NAME = "warungku_db";
    
    /**
     * Restore database dari file SQLite
     * Note: Method ini akan membuat backup otomatis sebelum restore
     * @param context Context
     * @param backupFile File backup yang akan di-restore
     * @return true jika berhasil, false jika gagal
     */
    public static boolean restoreDatabase(Context context, File backupFile) {
        if (backupFile == null || !backupFile.exists()) {
            return false;
        }
        
        try {
            // 1. Buat backup otomatis sebelum restore (safety)
            File autoBackup = DatabaseBackupUtils.backupDatabase(context);
            if (autoBackup == null) {
                // Jika gagal backup, tetap lanjutkan restore (user sudah konfirmasi)
                // Tapi log warning
                android.util.Log.w("DatabaseRestore", "Gagal membuat backup otomatis sebelum restore");
            }
            
            // 2. Close database connection
            AppDatabase db = AppDatabase.getDatabase(context);
            db.close();
            
            // 3. Get database file
            File dbFile = context.getDatabasePath(DB_NAME);
            File dbWalFile = new File(dbFile.getPath() + "-wal");
            File dbShmFile = new File(dbFile.getPath() + "-shm");
            
            // 4. Delete existing database files
            if (dbFile.exists()) dbFile.delete();
            if (dbWalFile.exists()) dbWalFile.delete();
            if (dbShmFile.exists()) dbShmFile.delete();
            
            // 5. Copy backup file ke database location
            FileInputStream fis = new FileInputStream(backupFile);
            FileOutputStream fos = new FileOutputStream(dbFile);
            FileChannel source = fis.getChannel();
            FileChannel destination = fos.getChannel();
            destination.transferFrom(source, 0, source.size());
            
            source.close();
            destination.close();
            fis.close();
            fos.close();
            
            // 6. Reopen database (akan dibuat otomatis oleh Room)
            AppDatabase.getDatabase(context);
            
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Restore database dari Uri (untuk file picker)
     * @param context Context
     * @param uri Uri dari file backup
     * @return true jika berhasil, false jika gagal
     */
    public static boolean restoreDatabaseFromUri(Context context, Uri uri) {
        if (uri == null) {
            return false;
        }
        
        try {
            // 1. Buat backup otomatis sebelum restore
            File autoBackup = DatabaseBackupUtils.backupDatabase(context);
            if (autoBackup == null) {
                android.util.Log.w("DatabaseRestore", "Gagal membuat backup otomatis sebelum restore");
            }
            
            // 2. Close database connection
            AppDatabase db = AppDatabase.getDatabase(context);
            db.close();
            
            // 3. Get database file
            File dbFile = context.getDatabasePath(DB_NAME);
            File dbWalFile = new File(dbFile.getPath() + "-wal");
            File dbShmFile = new File(dbFile.getPath() + "-shm");
            
            // 4. Delete existing database files
            if (dbFile.exists()) dbFile.delete();
            if (dbWalFile.exists()) dbWalFile.delete();
            if (dbShmFile.exists()) dbShmFile.delete();
            
            // 5. Copy dari Uri ke database location
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return false;
            }
            
            FileOutputStream fos = new FileOutputStream(dbFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            
            inputStream.close();
            fos.close();
            
            // 6. Reopen database
            AppDatabase.getDatabase(context);
            
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Validate backup file (check if it's a valid SQLite database)
     * @param file Backup file
     * @return true jika valid, false jika tidak
     */
    public static boolean isValidBackupFile(File file) {
        if (file == null || !file.exists() || !file.getName().endsWith(".db")) {
            return false;
        }
        
        // Check file size (minimal beberapa KB)
        if (file.length() < 1024) {
            return false;
        }
        
        // Check SQLite magic number (first 16 bytes should be "SQLite format 3\000")
        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] header = new byte[16];
            int read = fis.read(header);
            fis.close();
            
            if (read < 16) {
                return false;
            }
            
            String headerStr = new String(header);
            return headerStr.startsWith("SQLite format 3");
        } catch (IOException e) {
            return false;
        }
    }
}

