package com.alkahfprogrammer.warungku.data.model;

public class Invite {
    public String inviteId;
    public String warungId;
    public String ownerId;
    public String email;
    public String role; // "manager", "cashier" (staff dihapus)
    public String status; // "pending", "accepted", "expired"
    public long createdAt;
    public long expiresAt;
    public Long acceptedAt; // nullable
    public String acceptedBy; // nullable

    public Invite() {
        // Default constructor for Firestore
    }

    public Invite(String inviteId, String warungId, String ownerId, String email, String role) {
        this.inviteId = inviteId;
        this.warungId = warungId;
        this.ownerId = ownerId;
        this.email = email;
        this.role = role;
        this.status = "pending";
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000); // 7 days
        this.acceptedAt = null;
        this.acceptedBy = null;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    public boolean isPending() {
        return "pending".equals(status) && !isExpired();
    }
}

