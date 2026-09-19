package com.udhr.dto;

import com.udhr.model.Prescription;
import com.udhr.model.QueueEntry;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

// A queue entry awaiting pharmacy action, paired with the patient's
// currently active prescriptions so the Pharmacy Queue tab can show what
// needs to be dispensed without a separate patient lookup.
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PharmacyQueueItem {
    private QueueEntry queueEntry;
    private List<Prescription> activePrescriptions;
}
