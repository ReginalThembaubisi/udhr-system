package com.udhr.service;

import com.udhr.dto.StockAdjustmentRequest;
import com.udhr.dto.StockItemRequest;
import com.udhr.dto.StockReportResponse;
import com.udhr.model.*;
import com.udhr.repository.*;
import com.udhr.util.CsvUtil;
import com.udhr.util.DateRangeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    @Autowired
    private AuditService auditService;

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
        item.setExpiryDate(request.getExpiryDate());
        item.setQuantityOnHand(0);
        StockItem saved = stockItemRepository.save(item);

        int initialQuantity = request.getQuantityOnHand() != null ? request.getQuantityOnHand() : 0;
        if (initialQuantity > 0) {
            applyTransaction(saved, StockTransaction.Type.RECEIVED, initialQuantity, staff, "Initial stock on hand");
        }
        return saved;
    }

    /** How many rows one bulk import may hold. */
    public static final int MAX_BULK_ROWS = 500;

    /** Every problem in a bulk import, so the pharmacist can fix them all at once. */
    public static class BulkImportException extends RuntimeException {
        private final List<String> errors;

        public BulkImportException(List<String> errors) {
            super(errors.size() == 1 ? errors.get(0) : errors.size() + " rows need fixing before anything is added");
            this.errors = errors;
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    public record BulkImportResult(int created, int toppedUp) {}

    /**
     * Adds a whole stock list at once, e.g. pasted from a spreadsheet. A new
     * medicine becomes a stock item; one already tracked here has the quantity
     * received into it (and its expiry date and reorder level updated when
     * given). Every row is checked first and nothing is saved unless all of them
     * are fine, so a typo in row 40 never leaves half a list behind.
     */
    @Transactional
    public BulkImportResult bulkImport(List<StockItemRequest> rows, String staffNumber) {
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        Facility facility = staff.getFacility();

        List<String> errors = new ArrayList<>();
        if (rows == null || rows.isEmpty()) {
            throw new BulkImportException(List.of("There are no rows to add."));
        }
        if (rows.size() > MAX_BULK_ROWS) {
            throw new BulkImportException(List.of("Add at most " + MAX_BULK_ROWS + " rows at a time (this list has " + rows.size() + ")."));
        }
        Map<String, Integer> firstRowByName = new HashMap<>();
        for (int i = 0; i < rows.size(); i++) {
            StockItemRequest row = rows.get(i);
            int number = i + 1;
            String name = row.getMedicationName() == null ? "" : row.getMedicationName().trim();
            if (name.isEmpty()) {
                errors.add("Row " + number + ": the medication name is missing.");
                continue;
            }
            if (name.length() > 100) {
                errors.add("Row " + number + ": the medication name is longer than 100 characters.");
            }
            Integer earlier = firstRowByName.putIfAbsent(name.toLowerCase(Locale.ROOT), number);
            if (earlier != null) {
                errors.add("Row " + number + ": " + name + " is already in row " + earlier + ".");
            }
            if (row.getQuantityOnHand() != null && row.getQuantityOnHand() < 0) {
                errors.add("Row " + number + ": the quantity for " + name + " can't be negative.");
            }
            if (row.getReorderLevel() != null && row.getReorderLevel() < 0) {
                errors.add("Row " + number + ": the reorder level for " + name + " can't be negative.");
            }
            if (row.getExpiryDate() != null && row.getExpiryDate().isBefore(LocalDate.now())) {
                errors.add("Row " + number + ": " + name + " expired on " + row.getExpiryDate() + ", so it can't be added.");
            }
        }
        if (!errors.isEmpty()) {
            throw new BulkImportException(errors);
        }

        int created = 0;
        int toppedUp = 0;
        for (StockItemRequest row : rows) {
            String name = row.getMedicationName().trim();
            int quantity = row.getQuantityOnHand() != null ? row.getQuantityOnHand() : 0;
            StockItem existing = stockItemRepository.findByFacilityIdAndMedicationNameIgnoreCase(facility.getId(), name).orElse(null);
            if (existing == null) {
                StockItem item = new StockItem();
                item.setFacility(facility);
                item.setMedicationName(name);
                item.setUnit(row.getUnit() != null && !row.getUnit().isBlank() ? row.getUnit().trim() : "units");
                item.setReorderLevel(row.getReorderLevel() != null ? row.getReorderLevel() : 0);
                item.setExpiryDate(row.getExpiryDate());
                item.setQuantityOnHand(0);
                StockItem saved = stockItemRepository.save(item);
                if (quantity > 0) {
                    applyTransaction(saved, StockTransaction.Type.RECEIVED, quantity, staff, "Initial stock on hand (bulk import)");
                }
                created++;
            } else {
                if (row.getExpiryDate() != null) {
                    existing.setExpiryDate(row.getExpiryDate());
                }
                if (row.getReorderLevel() != null) {
                    existing.setReorderLevel(row.getReorderLevel());
                }
                if (quantity > 0) {
                    applyTransaction(existing, StockTransaction.Type.RECEIVED, quantity, staff, "Received (bulk import)");
                } else {
                    stockItemRepository.save(existing);
                }
                toppedUp++;
            }
        }
        // Each quantity received is already audited by applyTransaction; this
        // entry records the import as one action.
        auditService.recordCreate(staff, null, "STOCK_BULK_IMPORT", "Imported a stock list at " + facility.getName(),
                created + " new item(s), " + toppedUp + " existing item(s) updated");
        return new BulkImportResult(created, toppedUp);
    }

    @Transactional
    public StockItem receiveStock(StockAdjustmentRequest request, String staffNumber) {
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        StockItem item = stockItemRepository.findById(request.getStockItemId())
                .orElseThrow(() -> new RuntimeException("Stock item not found"));
        requireSameFacility(item, staff);

        int quantity = request.getQuantityChange() != null ? request.getQuantityChange() : 0;
        if (quantity <= 0) {
            throw new RuntimeException("Quantity received must be greater than zero");
        }

        // A delivery brings its own expiry date, so receiving stock updates the
        // shelf date to the batch just delivered. Receiving stock that has
        // already expired is refused outright — it should never reach the shelf.
        if (request.getExpiryDate() != null) {
            if (request.getExpiryDate().isBefore(LocalDate.now())) {
                throw new RuntimeException("Cannot receive stock that expired on " + request.getExpiryDate()
                        + " — check the batch and return it to the supplier");
            }
            item.setExpiryDate(request.getExpiryDate());
        }
        return applyTransaction(item, StockTransaction.Type.RECEIVED, quantity, staff, request.getNotes());
    }

    @Transactional
    public StockItem adjustStock(StockAdjustmentRequest request, String staffNumber) {
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        StockItem item = stockItemRepository.findById(request.getStockItemId())
                .orElseThrow(() -> new RuntimeException("Stock item not found"));
        requireSameFacility(item, staff);

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

    public List<StockTransaction> getTransactionHistory(Long stockItemId, String staffNumber) {
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        StockItem item = stockItemRepository.findById(stockItemId)
                .orElseThrow(() -> new RuntimeException("Stock item not found"));
        requireSameFacility(item, staff);
        return stockTransactionRepository.findByStockItemIdOrderByCreatedAtDesc(stockItemId);
    }

    // Stock item ids are sequential, so without this a pharmacist at one
    // facility could adjust or read another facility's shelf just by trying ids.
    private void requireSameFacility(StockItem item, Staff staff) {
        if (!item.getFacility().getId().equals(staff.getFacility().getId())) {
            throw new AccessDeniedException("This stock item belongs to another facility");
        }
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

        List<StockItem> expired = items.stream()
                .filter(StockItem::isExpired)
                .sorted(Comparator.comparing(StockItem::getExpiryDate))
                .collect(Collectors.toList());
        List<StockItem> expiringSoon = items.stream()
                .filter(StockItem::isExpiringSoon)
                .sorted(Comparator.comparing(StockItem::getExpiryDate))
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
                expired.size(),
                expired,
                expiringSoon.size(),
                expiringSoon,
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

        List<String> headers = List.of("Date", "Type", "Medication", "Expiry Date", "Quantity Change", "Unit", "Staff", "Notes");
        List<List<String>> rows = transactions.stream()
                .map(t -> List.of(
                        t.getCreatedAt().toString(),
                        t.getType().name(),
                        t.getStockItem().getMedicationName(),
                        t.getStockItem().getExpiryDate() != null ? t.getStockItem().getExpiryDate().toString() : "Not recorded",
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

        // Expired stock is never dispensed, whatever the quantity on hand says.
        // This is checked before the quantity parse on purpose: an item whose
        // dispensed quantity can't be parsed still must not be issued expired.
        if (item.isExpired()) {
            throw new RuntimeException("Cannot dispense " + item.getMedicationName()
                    + " — this stock expired on " + item.getExpiryDate()
                    + ". Write it off and dispense from a current batch.");
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
        // Captured before the change so the audit entry can show both sides —
        // "what was changed", not just that a change happened.
        String previousState = item.getQuantityOnHand() + " " + item.getUnit()
                + (item.getExpiryDate() != null ? ", expires " + item.getExpiryDate() : ", expiry not recorded");

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

        auditService.recordChange(staff, null, "STOCK_" + type.name(),
                String.format("%s %s of %s at %s%s",
                        quantityChange >= 0 ? "Added" : "Removed",
                        Math.abs(quantityChange) + " " + savedItem.getUnit(),
                        savedItem.getMedicationName(),
                        savedItem.getFacility().getName(),
                        notes != null && !notes.isBlank() ? " — " + notes : ""),
                previousState,
                savedItem.getQuantityOnHand() + " " + savedItem.getUnit()
                        + (savedItem.getExpiryDate() != null ? ", expires " + savedItem.getExpiryDate() : ", expiry not recorded"));

        // Fires once per dip below the reorder level, not on every
        // transaction while it stays low — resets above, via lowStockNotified,
        // whenever the item is next replenished past the reorder level.
        if (justCrossedIntoLowStock) {
            sendReorderAlerts(savedItem);
        }

        return savedItem;
    }

    private void sendReorderAlerts(StockItem item) {
        // The pharmacist manages stock and places orders, so the alert goes to
        // them. It used to go to the front desk, who can't act on it.
        List<Staff> pharmacists = staffRepository.findByFacilityIdAndRole(item.getFacility().getId(), "PHARMACIST");
        if (pharmacists.isEmpty()) {
            return;
        }

        String subject = "Reorder Alert: " + item.getMedicationName() + " low at " + item.getFacility().getName();
        String message = String.format(
                "UDHR Reorder Alert: %s is at %d %s at %s, at or below the reorder level of %d %s. Please arrange to restock.",
                item.getMedicationName(), item.getQuantityOnHand(), item.getUnit(), item.getFacility().getName(),
                item.getReorderLevel(), item.getUnit()
        );

        for (Staff pharmacist : pharmacists) {
            if (pharmacist.getContactNumber() != null && !pharmacist.getContactNumber().isBlank()) {
                africasTalkingService.sendSMS(pharmacist.getContactNumber(), message);
            }
            if (pharmacist.getEmail() != null && !pharmacist.getEmail().isBlank()) {
                System.out.println("[Email Dispatcher] Reorder alert queued for " + pharmacist.getStaffNumber() + ": " + subject);
            }
        }
    }
}
