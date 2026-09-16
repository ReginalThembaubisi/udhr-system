package com.udhr.model;

import jakarta.persistence.*;
import lombok.Data;
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
}
