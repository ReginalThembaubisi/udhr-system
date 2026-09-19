package com.udhr.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "visits")
public class Visit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @ManyToOne
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Column(name = "visit_date", nullable = false, updatable = false)
    private LocalDateTime visitDate;

    @Column(nullable = false)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // WAITING_VITALS -> VITALS_DONE -> WAITING_DOCTOR -> DIAGNOSED -> SENT_TO_PHARMACY / SELF_DISPENSED -> COMPLETE
    // (or REFERRED / DISCHARGED as alternate terminal states)
    @Column(nullable = false)
    private String status = "WAITING_VITALS";

    // "HOME", "DECEASED", "ABSCONDED", "TRANSFERRED" — only set once the
    // visit is explicitly closed out via /api/visits/discharge.
    @Column(name = "discharge_outcome")
    private String dischargeOutcome;

    @Column(name = "discharge_summary", columnDefinition = "TEXT")
    private String dischargeSummary;

    @Column(name = "follow_up_date")
    private LocalDate followUpDate;

    @ManyToOne
    @JoinColumn(name = "discharged_by")
    private Staff dischargedBy;

    @Column(name = "discharged_at")
    private LocalDateTime dischargedAt;

    @PrePersist
    protected void onCreate() {
        this.visitDate = LocalDateTime.now();
        if (this.status == null) {
            this.status = "WAITING_VITALS";
        }
    }
}
