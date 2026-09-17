package com.udhr.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "dispenses")
public class Dispense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    // Denormalized alongside the prescription link so this record surfaces
    // directly in the patient's cross-facility record, the same way a
    // Referral is patient-scoped rather than only reachable through a visit.
    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @ManyToOne
    @JoinColumn(name = "dispensed_by", nullable = false)
    private Staff dispensedBy;

    @Column(name = "quantity_dispensed", nullable = false)
    private String quantityDispensed;

    @Column(name = "days_supply")
    private Integer daysSupply;

    @Column(name = "pharmacy_notes", columnDefinition = "TEXT")
    private String pharmacyNotes;

    @Column(name = "dispensed_at", nullable = false, updatable = false)
    private LocalDateTime dispensedAt;

    @PrePersist
    protected void onCreate() {
        this.dispensedAt = LocalDateTime.now();
    }
}
