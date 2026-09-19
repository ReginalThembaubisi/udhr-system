package com.udhr.repository;

import com.udhr.model.StockItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StockItemRepository extends JpaRepository<StockItem, Long> {
    List<StockItem> findByFacilityIdOrderByMedicationNameAsc(Long facilityId);
    Optional<StockItem> findByFacilityIdAndMedicationNameIgnoreCase(Long facilityId, String medicationName);
    boolean existsByFacilityIdAndMedicationNameIgnoreCase(Long facilityId, String medicationName);
}
