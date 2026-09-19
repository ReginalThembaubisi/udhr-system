package com.udhr.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "immunizations")
public class Immunization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "vaccine_name", nullable = false)
    private String vaccineName; // e.g. "BCG", "OPV 0", "Rotavirus", "Measles 1" per the SA EPI schedule

    @Column(name = "dose_number")
    private Integer doseNumber;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate; // derived from date of birth per the EPI schedule

    @Column(name = "administered_date")
    private LocalDate administeredDate;

    @Column(nullable = false)
    private String status = "DUE"; // "DUE", "GIVEN", "MISSED"

    @ManyToOne
    @JoinColumn(name = "facility_id")
    private Facility facility;

    @ManyToOne
    @JoinColumn(name = "administered_by")
    private Staff administeredBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
