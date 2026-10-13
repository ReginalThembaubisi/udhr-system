package com.udhr.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "stock_items")
public class StockItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Column(name = "medication_name", nullable = false)
    private String medicationName;

    @Column(nullable = false)
    private String unit = "units"; // e.g. "tablets", "vials", "bottles"

    @Column(name = "quantity_on_hand", nullable = false)
    private Integer quantityOnHand = 0;

    // The expiry date of the stock currently on hand. The pharmacist needs
    // this on the stock screen so expired medication is never dispensed —
    // quantity alone doesn't tell them whether what's on the shelf is still
    // safe to issue. Left nullable so the column can be added to a table
    // that already has rows; those existing items show as "not recorded"
    // until someone captures a date, and are not treated as expired.
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    // Below this, the item shows as low stock so staff know to reorder.
    @Column(name = "reorder_level", nullable = false)
    private Integer reorderLevel = 0;

    // Tracks whether a reorder alert has already gone out for the current
    // low-stock dip, so admins get one notification per dip rather than one
    // per transaction while it stays low. Resets when stock is replenished
    // back above the reorder level.
    @Column(name = "low_stock_notified", nullable = false)
    private Boolean lowStockNotified = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    // Serialized onto the stock payload so the pharmacist's screen can flag
    // rows without the frontend re-deriving the rule. An item with no recorded
    // expiry date is never reported as expired.
    @Transient
    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    // Within 30 days of expiring (and not already expired) — the window where
    // staff should use it first or arrange a replacement.
    @Transient
    public boolean isExpiringSoon() {
        return expiryDate != null
                && !isExpired()
                && !expiryDate.isAfter(LocalDate.now().plusDays(30));
    }
}
