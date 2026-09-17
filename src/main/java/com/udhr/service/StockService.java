package com.udhr.service;

import com.udhr.dto.StockAdjustmentRequest;
import com.udhr.dto.StockItemRequest;
import com.udhr.dto.StockReportResponse;
import com.udhr.model.*;
import com.udhr.repository.*;
import com.udhr.util.CsvUtil;
import com.udhr.util.DateRangeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class StockService {

    private static final Pattern LEADING_INT = Pattern.compile("^\\s*(\\d+)");

    @Autowired
    private StockItemRepository stockItemRepository;

    @Autowired
    private StockTransactionRepository stockTransactionRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private AfricasTalkingService africasTalkingService;

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

    public StockReportResponse getReport(String staffNumber, String startDateStr, String endDateStr) {
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        Facility facility = staff.getFacility();
        LocalDate startDate = DateRangeUtil.parseOrNull(startDateStr);
        LocalDate endDate = DateRangeUtil.parseOrNull(endDateStr);

        // Current inventory state (what's tracked, what's low) isn't
        // meaningfully "date-ranged" — it reflects stock right now, not stock
        // as of some past window — so it stays unfiltered.
        List<StockItem> items = stockItemRepository.findByFacilityIdOrderByMedicationNameAsc(facility.getId());
        List<StockItem> lowStock = items.stream()
                .filter(i -> i.getQuantityOnHand() <= i.getReorderLevel())
                .sorted(Comparator.comparing(StockItem::getMedicationName))
                .collect(Collectors.toList());

        List<StockTransaction> transactions = getFilteredTransactions(facility, startDate, endDate);
        int totalReceived = transactions.stream()
                .filter(t -> t.getType() == StockTransaction.Type.RECEIVED)
                .mapToInt(StockTransaction::getQuantityChange)
                .sum();
        int totalDispensed = -transactions.stream()
                .filter(t -> t.getType() == StockTransaction.Type.DISPENSED)
                .mapToInt(StockTransaction::getQuantityChange)
                .sum();
        int totalWrittenOff = -transactions.stream()
                .filter(t -> t.getType() == StockTransaction.Type.ADJUSTED)
                .mapToInt(StockTransaction::getQuantityChange)
                .sum();
        List<StockTransaction> recent = transactions.stream().limit(10).collect(Collectors.toList());

        return new StockReportResponse(
                facility.getName(),
                items.size(),
                lowStock.size(),
                lowStock,
                totalReceived,
                totalDispensed,
                totalWrittenOff,
                recent
        );
    }

    public String exportCsv(String staffNumber, String startDateStr, String endDateStr) {
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        Facility facility = staff.getFacility();
        LocalDate startDate = DateRangeUtil.parseOrNull(startDateStr);
        LocalDate endDate = DateRangeUtil.parseOrNull(endDateStr);

        List<StockTransaction> transactions = getFilteredTransactions(facility, startDate, endDate);

        List<String> headers = List.of("Date", "Type", "Medication", "Quantity Change", "Unit", "Staff", "Notes");
        List<List<String>> rows = transactions.stream()
                .map(t -> List.of(
                        t.getCreatedAt().toString(),
                        t.getType().name(),
                        t.getStockItem().getMedicationName(),
                        String.valueOf(t.getQuantityChange()),
                        t.getStockItem().getUnit(),
                        t.getStaff().getFirstName() + " " + t.getStaff().getLastName(),
                        t.getNotes() != null ? t.getNotes() : ""
                ))
                .collect(Collectors.toList());

        return CsvUtil.buildCsv(headers, rows);
    }

    private List<StockTransaction> getFilteredTransactions(Facility facility, LocalDate startDate, LocalDate endDate) {
        return stockTransactionRepository.findByStockItem_FacilityIdOrderByCreatedAtDesc(facility.getId())
                .stream()
                .filter(t -> DateRangeUtil.isWithinRange(t.getCreatedAt(), startDate, endDate))
                .collect(Collectors.toList());
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

        boolean isLow = item.getQuantityOnHand() <= item.getReorderLevel();
        boolean justCrossedIntoLowStock = isLow && !Boolean.TRUE.equals(item.getLowStockNotified());
        item.setLowStockNotified(isLow);

        StockItem savedItem = stockItemRepository.save(item);

        StockTransaction tx = new StockTransaction();
        tx.setStockItem(savedItem);
        tx.setType(type);
        tx.setQuantityChange(quantityChange);
        tx.setStaff(staff);
        tx.setNotes(notes);
        stockTransactionRepository.save(tx);

        // Fires once per dip below the reorder level, not on every
        // transaction while it stays low — resets above, via lowStockNotified,
        // whenever the item is next replenished past the reorder level.
        if (justCrossedIntoLowStock) {
            sendReorderAlerts(savedItem);
        }

        return savedItem;
    }

    private void sendReorderAlerts(StockItem item) {
        List<Staff> admins = staffRepository.findByFacilityIdAndRole(item.getFacility().getId(), "ADMIN");
        if (admins.isEmpty()) {
            return;
        }

        String subject = "Reorder Alert: " + item.getMedicationName() + " low at " + item.getFacility().getName();
        String message = String.format(
                "UDHR Reorder Alert: %s is at %d %s at %s, at or below the reorder level of %d %s. Please arrange to restock.",
                item.getMedicationName(), item.getQuantityOnHand(), item.getUnit(), item.getFacility().getName(),
                item.getReorderLevel(), item.getUnit()
        );

        for (Staff admin : admins) {
            if (admin.getContactNumber() != null && !admin.getContactNumber().isBlank()) {
                africasTalkingService.sendSMS(admin.getContactNumber(), message);
            }
            if (admin.getEmail() != null && !admin.getEmail().isBlank()) {
                System.out.println(String.format("[Email Dispatcher] Sending email reorder alert to: %s | Subject: %s | Body: %s",
                        admin.getEmail(), subject, message));
            }
        }
    }
}
