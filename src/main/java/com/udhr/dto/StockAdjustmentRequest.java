package com.udhr.dto;

import lombok.Data;

@Data
public class StockAdjustmentRequest {
    private Long stockItemId;
    // Positive to receive stock, negative to write off (wastage, expiry, damage).
    private Integer quantityChange;
    private String notes;
}
