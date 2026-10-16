package com.udhr.service;

import com.udhr.model.Allergy;
import com.udhr.model.Staff;
import com.udhr.model.StockItem;
import com.udhr.repository.AllergyRepository;
import com.udhr.repository.StaffRepository;
import com.udhr.repository.StockItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Tells a prescriber whether their own facility's pharmacy has a medicine, and
 * if it is low or out, which medicines it does have instead.
 *
 * Alternatives come in two kinds: the same medicine under another name or
 * brand (safe like-for-like), and another medicine of the same class (the
 * prescriber checks the dose). Nothing the patient is recorded as allergic to
 * is ever suggested, and classes that must not be swapped for stock reasons
 * (see MedicationCatalog) get same-medicine suggestions only. Expired stock
 * counts as none.
 */
@Service
public class StockAvailabilityService {

    public enum Status { IN_STOCK, LOW, OUT_OF_STOCK, NOT_STOCKED }

    public record Alternative(String name, int onHand, String unit, String relation, String note) {}

    public record Availability(String medication, String generic, String drugClass, Status status,
                               int onHand, String unit, String stockName, List<Alternative> alternatives,
                               List<String> leftOutForAllergy) {}

    @Autowired private StaffRepository staffRepository;
    @Autowired private StockItemRepository stockItemRepository;
    @Autowired private AllergyRepository allergyRepository;
    @Autowired private AllergyCheckService allergyCheckService;
    @Autowired private MedicationCatalog catalog;

    public Availability check(String medication, Long patientId, String staffNumber) {
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        List<StockItem> stock = stockItemRepository.findByFacilityIdOrderByMedicationNameAsc(staff.getFacility().getId());
        List<Allergy> allergies = patientId != null ? allergyRepository.findByPatientId(patientId) : List.of();

        Optional<MedicationCatalog.Medicine> wanted = catalog.identify(medication);

        // Stock that is this medicine: same catalogue entry, or the same name typed.
        List<StockItem> matches = stock.stream()
                .filter(item -> isSameMedicine(item, medication, wanted))
                .sorted(Comparator.comparingInt(StockAvailabilityService::usable).reversed())
                .toList();
        StockItem best = matches.isEmpty() ? null : matches.get(0);
        int onHand = best == null ? 0 : usable(best);
        Status status = best == null ? Status.NOT_STOCKED
                : onHand == 0 ? Status.OUT_OF_STOCK
                : onHand <= (best.getReorderLevel() == null ? 0 : best.getReorderLevel()) ? Status.LOW
                : Status.IN_STOCK;

        List<Alternative> alternatives = new ArrayList<>();
        // Said out loud, so "no alternative" isn't mistaken for "nothing in stock".
        List<String> leftOutForAllergy = new ArrayList<>();
        if (status != Status.IN_STOCK) {
            for (StockItem item : stock) {
                if (item == best || usable(item) == 0) {
                    continue;
                }
                Optional<MedicationCatalog.Medicine> other = catalog.identify(item.getMedicationName());
                boolean candidate = isSameMedicine(item, medication, wanted)
                        || (wanted.isPresent() && !wanted.get().noSubstitute() && other.isPresent()
                            && other.get().drugClass().equals(wanted.get().drugClass()));
                if (candidate && !allergyCheckService.findConflicts(item.getMedicationName(), allergies).isEmpty()) {
                    leftOutForAllergy.add(item.getMedicationName());
                    continue;
                }
                if (isSameMedicine(item, medication, wanted)) {
                    alternatives.add(new Alternative(item.getMedicationName(), usable(item), item.getUnit(), "SAME_MEDICINE",
                            "Same medicine" + (wanted.isPresent() ? " (" + wanted.get().generic() + ")" : "")));
                } else if (wanted.isPresent() && !wanted.get().noSubstitute() && other.isPresent()
                        && other.get().drugClass().equals(wanted.get().drugClass())) {
                    alternatives.add(new Alternative(item.getMedicationName(), usable(item), item.getUnit(), "SAME_CLASS",
                            "Same class (" + wanted.get().drugClass() + "): check the dose"));
                }
            }
            alternatives.sort(Comparator.comparing(Alternative::relation, Comparator.reverseOrder())
                    .thenComparing(Comparator.comparingInt(Alternative::onHand).reversed()));
        }

        return new Availability(medication, wanted.map(MedicationCatalog.Medicine::generic).orElse(null),
                wanted.map(MedicationCatalog.Medicine::drugClass).orElse(null), status, onHand,
                best != null ? best.getUnit() : null, best != null ? best.getMedicationName() : null,
                alternatives.stream().limit(5).toList(), leftOutForAllergy);
    }

    private boolean isSameMedicine(StockItem item, String typed, Optional<MedicationCatalog.Medicine> wanted) {
        if (wanted.isPresent()) {
            return catalog.identify(item.getMedicationName()).map(m -> m.generic().equals(wanted.get().generic())).orElse(false);
        }
        // Unknown to the catalogue: match on the first word typed, e.g. "Allegex".
        String first = typed == null ? "" : typed.trim().split("\\s+")[0].toLowerCase(Locale.ROOT);
        return first.length() >= 3 && item.getMedicationName().toLowerCase(Locale.ROOT).startsWith(first);
    }

    // Expired stock can't be handed out, so it counts as none.
    private static int usable(StockItem item) {
        if (item.getExpiryDate() != null && item.getExpiryDate().isBefore(LocalDate.now())) {
            return 0;
        }
        return Math.max(0, item.getQuantityOnHand() == null ? 0 : item.getQuantityOnHand());
    }
}
