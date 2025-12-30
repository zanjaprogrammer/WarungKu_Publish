package com.zanjaprogrammer.warungku.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cash_flow")
public class CashFlow {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String type; // "IN" (SALE) or "OUT" (EXPENSE)
    public double amount;
    public String description;
    public long timestamp;
    public Integer productId; // Optional, linked to Product if it's a sale
    public Double profit; // Calculated at time of sale

    public CashFlow(String type, double amount, String description, long timestamp, Integer productId, Double profit) {
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.timestamp = timestamp;
        this.productId = productId;
        this.profit = profit;
    }
}
