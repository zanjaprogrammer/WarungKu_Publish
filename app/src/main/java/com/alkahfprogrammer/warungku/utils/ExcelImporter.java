package com.alkahfprogrammer.warungku.utils;

import android.content.Context;
import android.util.Log;
import com.alkahfprogrammer.warungku.data.entity.Product;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ExcelImporter {
    private static final String TAG = "ExcelImporter";
    
    public static class ImportResult {
        public int successCount;
        public int failCount;
        public List<String> errors;
        public List<Product> importedProducts;
        
        public ImportResult() {
            this.successCount = 0;
            this.failCount = 0;
            this.errors = new ArrayList<>();
            this.importedProducts = new ArrayList<>();
        }
    }
    
    /**
     * Import products from Excel file
     * @param context Android context
     * @param filePath Path to Excel file
     * @param existingProducts List of existing products (for duplicate check)
     * @return ImportResult with success/fail counts and errors
     */
    public static ImportResult importProducts(Context context, String filePath, List<Product> existingProducts) {
        ImportResult result = new ImportResult();
        
        try {
            InputStream inputStream = new FileInputStream(filePath);
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0);
            
            // Skip header row (row 0)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                // Skip row if first cell (nama produk) is empty
                Cell firstCell = row.getCell(0);
                if (firstCell == null || getCellValueAsString(firstCell) == null || getCellValueAsString(firstCell).trim().isEmpty()) {
                    continue; // Skip empty rows
                }
                
                try {
                    Product product = parseProductFromRow(row, i + 1, existingProducts, result);
                    if (product != null) {
                        result.importedProducts.add(product);
                        result.successCount++;
                    } else {
                        result.failCount++;
                    }
                } catch (Exception e) {
                    result.failCount++;
                    result.errors.add("Baris " + (i + 1) + ": " + e.getMessage());
                    Log.e(TAG, "Error parsing row " + (i + 1), e);
                }
            }
            
            workbook.close();
            inputStream.close();
            
        } catch (IOException e) {
            Log.e(TAG, "Import failed", e);
            result.errors.add("Error membaca file: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Import products from InputStream (for file picker)
     */
    public static ImportResult importProductsFromStream(Context context, InputStream inputStream, List<Product> existingProducts) {
        ImportResult result = new ImportResult();
        
        try {
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0);
            
            // Skip header row (row 0)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                try {
                    Product product = parseProductFromRow(row, i + 1, existingProducts, result);
                    if (product != null) {
                        result.importedProducts.add(product);
                        result.successCount++;
                    } else {
                        result.failCount++;
                    }
                } catch (Exception e) {
                    result.failCount++;
                    result.errors.add("Baris " + (i + 1) + ": " + e.getMessage());
                    Log.e(TAG, "Error parsing row " + (i + 1), e);
                }
            }
            
            workbook.close();
            
        } catch (IOException e) {
            Log.e(TAG, "Import failed", e);
            result.errors.add("Error membaca file: " + e.getMessage());
        }
        
        return result;
    }
    
    private static Product parseProductFromRow(Row row, int rowNumber, List<Product> existingProducts, ImportResult result) throws Exception {
        // Nama Produk (required)
        Cell nameCell = row.getCell(0);
        String name = getCellValueAsString(nameCell);
        if (name == null || name.trim().isEmpty()) {
            throw new Exception("Nama produk tidak boleh kosong");
        }
        name = name.trim();
        
        // Harga Beli (optional) - handle empty cell gracefully
        Cell buyPriceCell = row.getCell(1);
        Double buyPrice = null;
        if (buyPriceCell != null) {
            buyPrice = getCellValueAsDouble(buyPriceCell);
            if (buyPrice != null && buyPrice <= 0) {
                buyPrice = null; // Set null if invalid
            }
        }
        
        // Harga Jual (required)
        Cell sellPriceCell = row.getCell(2);
        Double sellPrice = null;
        if (sellPriceCell != null) {
            sellPrice = getCellValueAsDouble(sellPriceCell);
        }
        if (sellPrice == null || sellPrice <= 0) {
            throw new Exception("Harga jual harus lebih dari 0");
        }
        
        // Stok (optional, default 0) - handle empty cell gracefully
        Cell stockCell = row.getCell(3);
        Integer stock = 0;
        if (stockCell != null) {
            Integer stockValue = getCellValueAsInt(stockCell);
            if (stockValue != null && stockValue >= 0) {
                stock = stockValue;
            }
        }
        
        // Stok Minimum (optional, default 0) - handle empty cell gracefully
        Cell minStockCell = row.getCell(4);
        Integer minStock = 0;
        if (minStockCell != null) {
            Integer minStockValue = getCellValueAsInt(minStockCell);
            if (minStockValue != null && minStockValue >= 0) {
                minStock = minStockValue;
            }
        }
        
        // Barcode (optional) - handle empty cell gracefully
        Cell barcodeCell = row.getCell(5);
        String barcode = null;
        if (barcodeCell != null) {
            String barcodeValue = getCellValueAsString(barcodeCell);
            if (barcodeValue != null) {
                barcode = barcodeValue.trim();
                if (barcode.isEmpty()) {
                    barcode = null;
                } else {
                    // Check duplicate barcode
                    for (Product existing : existingProducts) {
                        if (existing.barcode != null && existing.barcode.equals(barcode)) {
                            // Skip if duplicate, but don't throw error (will update by name instead)
                            break;
                        }
                    }
                }
            }
        }
        
        // Check if product already exists (by name) - will update instead of create
        Product existingProduct = null;
        for (Product p : existingProducts) {
            if (p.name != null && p.name.equalsIgnoreCase(name)) {
                existingProduct = p;
                break;
            }
        }
        
        if (existingProduct != null) {
            // Update existing product
            existingProduct.sellPrice = sellPrice;
            existingProduct.buyPrice = buyPrice;
            existingProduct.currentStock = stock;
            existingProduct.minStock = minStock;
            if (barcode != null && !barcode.isEmpty()) {
                existingProduct.barcode = barcode;
            }
            return existingProduct;
        } else {
            // Create new product
            Product newProduct = new Product(name, sellPrice, buyPrice, stock, minStock);
            if (barcode != null && !barcode.isEmpty()) {
                newProduct.barcode = barcode;
            }
            return newProduct;
        }
    }
    
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        
        try {
            switch (cell.getCellType()) {
                case STRING:
                    String str = cell.getStringCellValue();
                    return str != null ? str.trim() : null;
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getDateCellValue().toString();
                    } else {
                        // Convert to string without decimal if it's a whole number
                        double numValue = cell.getNumericCellValue();
                        if (numValue == (long) numValue) {
                            return String.valueOf((long) numValue);
                        } else {
                            return String.valueOf(numValue);
                        }
                    }
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    try {
                        // Try to get formula result as string
                        return cell.getStringCellValue();
                    } catch (Exception e) {
                        try {
                            // Try as numeric
                            double numValue = cell.getNumericCellValue();
                            if (numValue == (long) numValue) {
                                return String.valueOf((long) numValue);
                            } else {
                                return String.valueOf(numValue);
                            }
                        } catch (Exception ex) {
                            return cell.getCellFormula();
                        }
                    }
                case BLANK:
                    return null;
                default:
                    return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading cell as string", e);
            return null;
        }
    }
    
    private static Double getCellValueAsDouble(Cell cell) {
        if (cell == null) return null;
        
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return cell.getNumericCellValue();
                case STRING:
                    String str = cell.getStringCellValue();
                    if (str == null || str.trim().isEmpty()) return null;
                    str = str.trim();
                    try {
                        // Remove currency symbols and commas
                        str = str.replace("Rp", "").replace("rp", "").replace(",", "").replace(".", "").trim();
                        if (str.isEmpty()) return null;
                        return Double.parseDouble(str);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                case FORMULA:
                    try {
                        return cell.getNumericCellValue();
                    } catch (Exception e) {
                        // Try to get as string and parse
                        try {
                            String formulaStr = cell.getStringCellValue();
                            if (formulaStr != null && !formulaStr.trim().isEmpty()) {
                                formulaStr = formulaStr.replace("Rp", "").replace("rp", "").replace(",", "").replace(".", "").trim();
                                if (!formulaStr.isEmpty()) {
                                    return Double.parseDouble(formulaStr);
                                }
                            }
                        } catch (Exception ex) {
                            // Ignore
                        }
                        return null;
                    }
                case BLANK:
                    return null;
                default:
                    return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading cell as double", e);
            return null;
        }
    }
    
    private static Integer getCellValueAsInt(Cell cell) {
        if (cell == null) return null;
        
        try {
            // Try direct numeric first
            if (cell.getCellType() == CellType.NUMERIC) {
                double numValue = cell.getNumericCellValue();
                return (int) numValue;
            }
            
            // Try as double and convert
            Double value = getCellValueAsDouble(cell);
            if (value == null) return null;
            return value.intValue();
        } catch (Exception e) {
            Log.e(TAG, "Error reading cell as int", e);
            return null;
        }
    }
}

