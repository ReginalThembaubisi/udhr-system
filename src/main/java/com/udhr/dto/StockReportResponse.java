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
    private int totalUnitsReceived;
    private int totalUnitsDispensed;
    private int totalUnitsWrittenOff;
    private List<StockTransaction> recentTransactions;
}
