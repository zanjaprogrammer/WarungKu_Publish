package com.zanjaprogrammer.warungku.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CurrencyFormatter {
    private static final DecimalFormat formatter;
    
    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("id-ID"));
        symbols.setCurrencySymbol("Rp ");
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        
        formatter = new DecimalFormat("¤#,##0", symbols);
    }
    
    public static String format(double amount) {
        return formatter.format(amount);
    }
    
    public static String format(int amount) {
        return formatter.format(amount);
    }
}