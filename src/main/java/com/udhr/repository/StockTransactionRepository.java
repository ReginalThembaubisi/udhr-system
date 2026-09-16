package com.udhr.repository;

import com.udhr.model.StockTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {
    List<StockTransaction> findByStockItemIdOrderByCreatedAtDesc(Long stockItemId);
}
