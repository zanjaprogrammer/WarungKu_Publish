package com.alkahfprogrammer.warungku.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DatabaseBackupUtils {
    
    private static final String DB_NAME = "warungku_db";
    private static final String BACKUP_FOLDER = "WarungKu_Backup";
    
    /**
     * Backup database SQLite file ke app's external files directory
     * Tidak perlu permission untuk Android 10+
     * @return File path jika berhasil, null jika gagal
     */
    public static File backupDatabase(Context context) {
        try {
            // Get database file
            File dbFile = context.getDatabasePath(DB_NAME);
            if (!dbFile.exists()) {
                return null;
            }
            
            // Create backup folder di app's external files directory
            File backupFolder = new File(context.getExternalFilesDir(null), BACKUP_FOLDER);
            if (!backupFolder.exists()) {
                backupFolder.mkdirs();
            }
            
            // Create backup file dengan timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
            String backupFileName = "warungku_backup_" + timestamp + ".db";
            File backupFile = new File(backupFolder, backupFileName);
            
            // Copy database file
            FileInputStream fis = new FileInputStream(dbFile);
            FileOutputStream fos = new FileOutputStream(backupFile);
            FileChannel source = fis.getChannel();
            FileChannel destination = fos.getChannel();
            destination.transferFrom(source, 0, source.size());
            
            source.close();
            destination.close();
            fis.close();
            fos.close();
            
            return backupFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Share backup file via Intent (Google Drive, Email, dll)
     */
    public static Intent getShareIntent(Context context, File backupFile) {
        if (backupFile == null || !backupFile.exists()) {
            return null;
        }
        
        Uri fileUri = FileProvider.getUriForFile(
            context,
            context.getPackageName() + ".fileprovider",
            backupFile
        );
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/octet-stream");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "WarungKu Backup - " + backupFile.getName());
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Backup data WarungKu");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        return shareIntent;
    }
}

