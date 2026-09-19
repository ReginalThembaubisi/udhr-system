package com.udhr.util;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Shared by every admin facility report (stock, dispensing, referrals,
// prescriptions, lab results) to filter their event lists against an
// optional admin-picked date range before tallying. Either bound (or both)
// may be null, meaning "no limit on that side" — so omitting both keeps the
// existing all-time behavior every report had before this filter existed.
public class DateRangeUtil {

    public static boolean isWithinRange(LocalDateTime timestamp, LocalDate startDate, LocalDate endDate) {
        if (timestamp == null) {
            return false;
        }
        LocalDate date = timestamp.toLocalDate();
        if (startDate != null && date.isBefore(startDate)) {
            return false;
        }
        if (endDate != null && date.isAfter(endDate)) {
            return false;
        }
        return true;
    }

    public static LocalDate parseOrNull(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) {
            return null;
        }
        return LocalDate.parse(isoDate);
    }
}
