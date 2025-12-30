package com.alkahfprogrammer.warungku.auth;

import com.alkahfprogrammer.warungku.data.model.User;

/**
 * PermissionManager - Simplified version
 * All methods return true (all features accessible without authentication)
 */
public class PermissionManager {
    
    // Methods dengan User object (backward compatibility)
    public static boolean canSell(User user) {
        return true;
    }

    public static boolean canAddProduct(User user) {
        return true;
    }

    public static boolean canEditProduct(User user) {
        return true;
    }

    public static boolean canDeleteProduct(User user) {
        return true;
    }

    public static boolean canRestock(User user) {
        return true;
    }

    public static boolean canViewStock(User user) {
        return true;
    }

    public static boolean canViewReports(User user) {
        return true;
    }

    public static boolean canExportImport(User user) {
        return true;
    }

    public static boolean canManageEmployees(User user) {
        return true; // Feature removed, but keep method for compatibility
    }

    public static boolean canChangePrice(User user) {
        return true;
    }

    public static boolean canAddExpense(User user) {
        return true;
    }

    public static boolean canBackupRestore(User user) {
        return true;
    }

    public static boolean canAccessSettings(User user) {
        return true;
    }
    
    // Methods dengan role string (all return true)
    public static boolean canSell(String role) {
        return true;
    }

    public static boolean canAddProduct(String role) {
        return true;
    }

    public static boolean canEditProduct(String role) {
        return true;
    }

    public static boolean canDeleteProduct(String role) {
        return true;
    }

    public static boolean canRestock(String role) {
        return true;
    }

    public static boolean canViewStock(String role) {
        return true;
    }

    public static boolean canViewReports(String role) {
        return true;
    }

    public static boolean canExportImport(String role) {
        return true;
    }

    public static boolean canManageEmployees(String role) {
        return true; // Feature removed, but keep method for compatibility
    }

    public static boolean canChangePrice(String role) {
        return true;
    }

    public static boolean canAddExpense(String role) {
        return true;
    }

    public static boolean canBackupRestore(String role) {
        return true;
    }

    public static boolean canAccessSettings(String role) {
        return true;
    }
    
    // Additional permission checks
    public static boolean canAccessMoney(String role) {
        return true;
    }
    
    public static boolean canAccessSummary(String role) {
        return true;
    }
    
    public static boolean canAccessShoppingList(String role) {
        return true;
    }
}
