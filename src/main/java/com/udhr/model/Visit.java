package com.udhr.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    // A visit is the unit of a care episode. It stays ACTIVE while the
    // patient is being seen, and closes out one of two ways: a normal
    // discharge, or a referral to another facility (see Referral).
    public enum Status {
        ACTIVE, DISCHARGED, REFERRED
    }

    public enum DischargeOutcome {
        HOME, DECEASED, ABSCONDED, TRANSFERRED
    }

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "discharge_outcome")
    private DischargeOutcome dischargeOutcome;

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
    }
}
