package com.udhr.dto;

import lombok.Data;

@Data
public class StockItemRequest {
    private String medicationName;
    private String unit;
    private Integer quantityOnHand;
    private Integer reorderLevel;
}
