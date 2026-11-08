package com.udhr.util;

import com.udhr.model.Prescription;

import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * How much to hand over for a prescription: the amount per dose times doses a
 * day times the number of days, e.g. "2 tablets", "Twice daily" for 5 days is
 * "20 tablets". Empty when it can't be worked out safely (a liquid dose in ml,
 * "as needed"); the pharmacist then types the quantity.
 */
public final class DispenseQuantity {

    private static final Pattern DOSE = Pattern.compile("^\\s*(\\d+)\\s*([A-Za-z]+)?");
    // Units counted one by one. Anything else (ml, mg) is not a count of items.
    private static final java.util.Set<String> COUNTABLE = java.util.Set.of(
            "tablet", "tablets", "tab", "tabs", "capsule", "capsules", "cap", "caps", "sachet", "sachets",
            "puff", "puffs", "drop", "drops", "vial", "vials", "patch", "patches", "suppository", "suppositories");

    private DispenseQuantity() {}

    public static Optional<String> total(Prescription prescription) {
        if (prescription == null || prescription.getDosage() == null) {
            return Optional.empty();
        }
        Matcher dose = DOSE.matcher(prescription.getDosage());
        if (!dose.find()) {
            return Optional.empty();
        }
        int perDose = Integer.parseInt(dose.group(1));
        String unit = dose.group(2) == null ? "" : dose.group(2).toLowerCase(Locale.ROOT);
        if (!unit.isEmpty() && !COUNTABLE.contains(unit)) {
            return Optional.empty();
        }
        Integer perDay = timesPerDay(prescription.getFrequency());
        long days = days(prescription);
        if (perDose <= 0 || perDay == null || days <= 0) {
            return Optional.empty();
        }
        long count = perDose * perDay * days;
        return Optional.of(count + " " + plural(unit.isEmpty() ? "tablet" : unit, count));
    }

    public static long days(Prescription prescription) {
        if (prescription.getStartDate() == null || prescription.getEndDate() == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(prescription.getStartDate(), prescription.getEndDate());
    }

    static Integer timesPerDay(String frequency) {
        if (frequency == null) {
            return null;
        }
        String f = frequency.toLowerCase(Locale.ROOT);
        if (f.contains("as needed") || f.contains("prn") || f.contains("when needed")) return null;
        if (f.contains("four times") || f.contains("qid") || f.contains("qds")) return 4;
        if (f.contains("three times") || f.contains("tds") || f.contains("tid") || f.contains("with meals")) return 3;
        if (f.contains("twice") || f.contains("bd") || f.contains("bid")) return 2;
        if (f.contains("once") || f.contains("daily") || f.contains("at night") || f.contains("nocte")) return 1;
        return null;
    }

    private static String plural(String unit, long count) {
        String one = switch (unit) {
            case "tab", "tabs", "tablets" -> "tablet";
            case "cap", "caps", "capsules" -> "capsule";
            case "patches" -> "patch";
            case "suppositories" -> "suppository";
            default -> unit.endsWith("s") ? unit.substring(0, unit.length() - 1) : unit;
        };
        if (count == 1) {
            return one;
        }
        return switch (one) {
            case "patch" -> "patches";
            case "suppository" -> "suppositories";
            default -> one + "s";
        };
    }
}
