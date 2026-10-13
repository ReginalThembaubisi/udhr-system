package com.udhr.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class StockAdjustmentRequest {
    private Long stockItemId;
    // Positive to receive stock, negative to write off (wastage, expiry, damage).
    private Integer quantityChange;
    private String notes;
    // Expiry date of the batch being received. Optional: when supplied on a
    // receive, it replaces the item's recorded expiry date so the shelf date
    // reflects what was most recently delivered.
    private LocalDate expiryDate;
}
