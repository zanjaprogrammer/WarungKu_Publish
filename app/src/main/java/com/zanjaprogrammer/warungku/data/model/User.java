package com.zanjaprogrammer.warungku.data.model;

public class User {
    public String userId;
    public String email;
    public String name;
    public String role; // "owner", "manager", "cashier" (staff dihapus)
    public String warungId;
    public long createdAt;
    public boolean isActive;

    public User() {
        // Default constructor for Firestore
    }

    public User(String userId, String email, String name, String role, String warungId) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.role = role;
        this.warungId = warungId;
        this.createdAt = System.currentTimeMillis();
        this.isActive = true;
    }

    public boolean isOwner() {
        return "owner".equals(role);
    }

    public boolean isManager() {
        return "manager".equals(role);
    }

    public boolean isCashier() {
        return "cashier".equals(role);
    }
}

