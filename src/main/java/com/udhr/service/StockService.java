package com.udhr.service;

import com.udhr.dto.StockAdjustmentRequest;
import com.udhr.dto.StockItemRequest;
import com.udhr.model.*;
import com.udhr.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class StockService {

    private static final Pattern LEADING_INT = Pattern.compile("^\\s*(\\d+)");

    @Autowired
    private StockItemRepository stockItemRepository;

    @Autowired
    private StockTransactionRepository stockTransactionRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Transactional
    public StockItem addStockItem(StockItemRequest request, String staffNumber) {
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        Facility facility = staff.getFacility();

        if (stockItemRepository.existsByFacilityIdAndMedicationNameIgnoreCase(facility.getId(), request.getMedicationName())) {
            throw new RuntimeException("This medication is already tracked at your facility — use Receive Stock to add quantity instead");
        }

        StockItem item = new StockItem();
        item.setFacility(facility);
        item.setMedicationName(request.getMedicationName());
        item.setUnit(request.getUnit() != null && !request.getUnit().isBlank() ? request.getUnit() : "units");
        item.setReorderLevel(request.getReorderLevel() != null ? request.getReorderLevel() : 0);
        item.setQuantityOnHand(0);
        StockItem saved = stockItemRepository.save(item);

        int initialQuantity = request.getQuantityOnHand() != null ? request.getQuantityOnHand() : 0;
        if (initialQuantity > 0) {
            applyTransaction(saved, StockTransaction.Type.RECEIVED, initialQuantity, staff, "Initial stock on hand");
        }
        return saved;
    }

    @Transactional
    public StockItem receiveStock(StockAdjustmentRequest request, String staffNumber) {
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        StockItem item = stockItemRepository.findById(request.getStockItemId())
                .orElseThrow(() -> new RuntimeException("Stock item not found"));

        int quantity = request.getQuantityChange() != null ? request.getQuantityChange() : 0;
        if (quantity <= 0) {
            throw new RuntimeException("Quantity received must be greater than zero");
        }
        return applyTransaction(item, StockTransaction.Type.RECEIVED, quantity, staff, request.getNotes());
    }

    @Transactional
    public StockItem adjustStock(StockAdjustmentRequest request, String staffNumber) {
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        StockItem item = stockItemRepository.findById(request.getStockItemId())
                .orElseThrow(() -> new RuntimeException("Stock item not found"));

        int change = request.getQuantityChange() != null ? request.getQuantityChange() : 0;
        if (change == 0) {
            throw new RuntimeException("Adjustment quantity cannot be zero");
        }
        if (change > 0) {
            return applyTransaction(item, StockTransaction.Type.RECEIVED, change, staff, request.getNotes());
        }
        if (item.getQuantityOnHand() + change < 0) {
            throw new RuntimeException("Adjustment would take stock below zero — only " + item.getQuantityOnHand() + " " + item.getUnit() + " on hand");
        }
        return applyTransaction(item, StockTransaction.Type.ADJUSTED, change, staff, request.getNotes());
    }

    public List<StockItem> getStockForFacility(String staffNumber) {
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        return stockItemRepository.findByFacilityIdOrderByMedicationNameAsc(staff.getFacility().getId());
    }

    public List<StockTransaction> getTransactionHistory(Long stockItemId) {
        return stockTransactionRepository.findByStockItemIdOrderByCreatedAtDesc(stockItemId);
    }

    // Called when a prescription is dispensed. Best-effort: if this facility
    // isn't tracking the medication, or the dispensed quantity can't be
    // parsed as a number, dispensing proceeds without touching stock — not
    // every facility will have inventory set up for every medication.
    @Transactional
    public void decrementForDispense(Facility facility, String medicationName, String quantityDispensedText, Staff staff, Long prescriptionId) {
        StockItem item = stockItemRepository.findByFacilityIdAndMedicationNameIgnoreCase(facility.getId(), medicationName)
                .orElse(null);
        if (item == null) {
            return;
        }

        Matcher matcher = LEADING_INT.matcher(quantityDispensedText == null ? "" : quantityDispensedText);
        if (!matcher.find()) {
            return;
        }
        int quantity = Integer.parseInt(matcher.group(1));

        if (item.getQuantityOnHand() < quantity) {
            throw new RuntimeException("Insufficient stock: only " + item.getQuantityOnHand() + " " + item.getUnit()
                    + " of " + item.getMedicationName() + " on hand at " + facility.getName());
        }

        applyTransaction(item, StockTransaction.Type.DISPENSED, -quantity, staff, "Dispensed for prescription #" + prescriptionId);
    }

    private StockItem applyTransaction(StockItem item, StockTransaction.Type type, int quantityChange, Staff staff, String notes) {
        item.setQuantityOnHand(item.getQuantityOnHand() + quantityChange);
        item.setUpdatedAt(LocalDateTime.now());
        StockItem savedItem = stockItemRepository.save(item);

        StockTransaction tx = new StockTransaction();
        tx.setStockItem(savedItem);
        tx.setType(type);
        tx.setQuantityChange(quantityChange);
        tx.setStaff(staff);
        tx.setNotes(notes);
        stockTransactionRepository.save(tx);

        return savedItem;
    }
}
