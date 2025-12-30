package com.zanjaprogrammer.warungku.data.model;

public class Warung {
    public String warungId;
    public String name;
    public String ownerId;
    public long createdAt;
    public WarungSettings settings;

    public Warung() {
        // Default constructor for Firestore
    }

    public Warung(String warungId, String name, String ownerId) {
        this.warungId = warungId;
        this.name = name;
        this.ownerId = ownerId;
        this.createdAt = System.currentTimeMillis();
        this.settings = new WarungSettings();
    }

    public static class WarungSettings {
        public String currency = "IDR";
        public String timezone = "Asia/Jakarta";

        public WarungSettings() {
        }
    }
}

