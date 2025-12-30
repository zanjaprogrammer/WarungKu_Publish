package com.zanjaprogrammer.warungku.utils;

import android.content.Context;
import android.util.Log;
import com.zanjaprogrammer.warungku.data.entity.Product;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExcelExporter {
    private static final String TAG = "ExcelExporter";
    
    public static class ExportResult {
        public boolean success;
        public String filePath;
        public String errorMessage;
        
        public ExportResult(boolean success, String filePath, String errorMessage) {
            this.success = success;
            this.filePath = filePath;
            this.errorMessage = errorMessage;
        }
    }
    
    /**
     * Export products to Excel file
     * @param context Android context
     * @param products List of products to export
     * @return ExportResult with success status and file path
     */
    public static ExportResult exportProducts(Context context, List<Product> products) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Produk");
        
        try {
            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Nama Produk", "Harga Beli", "Harga Jual", "Stok", "Stok Minimum", "Barcode"};
            
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Create data rows
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setAlignment(HorizontalAlignment.LEFT);
            
            for (int i = 0; i < products.size(); i++) {
                Product product = products.get(i);
                Row row = sheet.createRow(i + 1);
                
                // Nama Produk
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(product.name != null ? product.name : "");
                cell0.setCellStyle(dataStyle);
                
                // Harga Beli
                Cell cell1 = row.createCell(1);
                if (product.buyPrice != null) {
                    cell1.setCellValue(product.buyPrice);
                } else {
                    cell1.setCellValue("");
                }
                cell1.setCellStyle(dataStyle);
                
                // Harga Jual
                Cell cell2 = row.createCell(2);
                cell2.setCellValue(product.sellPrice);
                cell2.setCellStyle(dataStyle);
                
                // Stok
                Cell cell3 = row.createCell(3);
                cell3.setCellValue(product.currentStock);
                cell3.setCellStyle(dataStyle);
                
                // Stok Minimum
                Cell cell4 = row.createCell(4);
                cell4.setCellValue(product.minStock);
                cell4.setCellStyle(dataStyle);
                
                // Barcode
                Cell cell5 = row.createCell(5);
                cell5.setCellValue(product.barcode != null ? product.barcode : "");
                cell5.setCellStyle(dataStyle);
            }
            
            // Set column widths (autoSizeColumn doesn't work on Android due to java.awt dependency)
            // Using fixed widths instead
            sheet.setColumnWidth(0, 15000); // Nama Produk
            sheet.setColumnWidth(1, 8000);  // Harga Beli
            sheet.setColumnWidth(2, 8000);  // Harga Jual
            sheet.setColumnWidth(3, 5000);  // Stok
            sheet.setColumnWidth(4, 8000);  // Stok Minimum
            sheet.setColumnWidth(5, 12000); // Barcode
            
            // Create file
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String timestamp = sdf.format(new Date());
            String fileName = "WarungKu_Products_" + timestamp + ".xlsx";
            
            File downloadsDir = new File(context.getExternalFilesDir(null), "Downloads");
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }
            
            File file = new File(downloadsDir, fileName);
            FileOutputStream fileOut = new FileOutputStream(file);
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();
            
            Log.d(TAG, "Export successful: " + file.getAbsolutePath());
            return new ExportResult(true, file.getAbsolutePath(), null);
            
        } catch (IOException e) {
            Log.e(TAG, "Export failed", e);
            try {
                workbook.close();
            } catch (IOException ex) {
                Log.e(TAG, "Error closing workbook", ex);
            }
            return new ExportResult(false, null, e.getMessage());
        }
    }
    
    /**
     * Generate template Excel file for import
     * @param context Android context
     * @return ExportResult with success status and file path
     */
    public static ExportResult generateTemplate(Context context) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Template Import Produk");
        
        try {
            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Nama Produk", "Harga Beli", "Harga Jual", "Stok", "Stok Minimum", "Barcode"};
            
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Create example rows
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setAlignment(HorizontalAlignment.LEFT);
            
            // Example 1
            Row exampleRow1 = sheet.createRow(1);
            exampleRow1.createCell(0).setCellValue("Teh Botol");
            exampleRow1.createCell(1).setCellValue(3000);
            exampleRow1.createCell(2).setCellValue(5000);
            exampleRow1.createCell(3).setCellValue(50);
            exampleRow1.createCell(4).setCellValue(10);
            exampleRow1.createCell(5).setCellValue("8999999025340");
            
            // Example 2
            Row exampleRow2 = sheet.createRow(2);
            exampleRow2.createCell(0).setCellValue("Indomie Goreng");
            exampleRow2.createCell(1).setCellValue(2500);
            exampleRow2.createCell(2).setCellValue(3500);
            exampleRow2.createCell(3).setCellValue(100);
            exampleRow2.createCell(4).setCellValue(20);
            exampleRow2.createCell(5).setCellValue("");
            
            // Add note row
            Row noteRow = sheet.createRow(4);
            Cell noteCell = noteRow.createCell(0);
            noteCell.setCellValue("Catatan:");
            CellStyle noteStyle = workbook.createCellStyle();
            Font noteFont = workbook.createFont();
            noteFont.setItalic(true);
            noteStyle.setFont(noteFont);
            noteCell.setCellStyle(noteStyle);
            
            Row noteRow2 = sheet.createRow(5);
            noteRow2.createCell(0).setCellValue("- Nama Produk dan Harga Jual wajib diisi");
            Row noteRow3 = sheet.createRow(6);
            noteRow3.createCell(0).setCellValue("- Harga Beli, Stok, Stok Minimum, dan Barcode opsional");
            Row noteRow4 = sheet.createRow(7);
            noteRow4.createCell(0).setCellValue("- Jika produk sudah ada (berdasarkan nama), akan diupdate");
            
            // Set column widths (autoSizeColumn doesn't work on Android due to java.awt dependency)
            // Using fixed widths instead
            sheet.setColumnWidth(0, 15000); // Nama Produk
            sheet.setColumnWidth(1, 8000);  // Harga Beli
            sheet.setColumnWidth(2, 8000);  // Harga Jual
            sheet.setColumnWidth(3, 5000);  // Stok
            sheet.setColumnWidth(4, 8000);  // Stok Minimum
            sheet.setColumnWidth(5, 12000); // Barcode
            
            // Create file
            String fileName = "WarungKu_Import_Template.xlsx";
            File downloadsDir = new File(context.getExternalFilesDir(null), "Downloads");
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }
            
            File file = new File(downloadsDir, fileName);
            FileOutputStream fileOut = new FileOutputStream(file);
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();
            
            Log.d(TAG, "Template generated: " + file.getAbsolutePath());
            return new ExportResult(true, file.getAbsolutePath(), null);
            
        } catch (IOException e) {
            Log.e(TAG, "Template generation failed", e);
            try {
                workbook.close();
            } catch (IOException ex) {
                Log.e(TAG, "Error closing workbook", ex);
            }
            return new ExportResult(false, null, e.getMessage());
        }
    }
}

