package com.udhr.dto;

import com.udhr.model.Visit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// One row on the admin front desk's "today's visitors" list — the visit
// plus whether this is the patient's first-ever visit (new) or not
// (returning), so admin can keep a record of who came in today and why.
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CheckInSummaryResponse {
    private Visit visit;
    private boolean firstVisit;
}
