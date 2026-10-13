package com.udhr.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class StockItemRequest {
    private String medicationName;
    private String unit;
    private Integer quantityOnHand;
    private Integer reorderLevel;
    private LocalDate expiryDate;
}
