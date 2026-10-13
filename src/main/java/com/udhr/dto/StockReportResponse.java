package com.udhr.dto;

import com.udhr.model.StockItem;
import com.udhr.model.StockTransaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockReportResponse {
    private String facilityName;
    private int totalMedicationsTracked;
    private int lowStockCount;
    private List<StockItem> lowStockItems;
    // Expired stock still sitting on the shelf, and stock due to expire within
    // 30 days — both need the admin's attention, the first to write off, the
    // second to use first or replace.
    private int expiredCount;
    private List<StockItem> expiredItems;
    private int expiringSoonCount;
    private List<StockItem> expiringSoonItems;
    private int totalUnitsReceived;
    private int totalUnitsDispensed;
    private int totalUnitsWrittenOff;
    private List<StockTransaction> recentTransactions;
}
