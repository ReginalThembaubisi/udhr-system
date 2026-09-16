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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Data
@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Permanent internal identifier assigned at first registration. This is
    // the record's true identity — unlike a national ID number, it exists
    // from the moment a patient (including a newborn) is first seen, and
    // never changes even after an ID number or passport number is captured
    // later.
    @Column(name = "uhid", unique = true, nullable = false, updatable = false, length = 24)
    private String uhid;

    // A national ID may not exist yet (newborns, undocumented patients), so
    // this can no longer be required. It is still unique when present.
    @Column(name = "id_number", unique = true)
    private String idNumber;

    @Column(name = "passport_number", unique = true)
    private String passportNumber;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false)
    private String gender; // "MALE", "FEMALE", "OTHER"

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "email")
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    // Links a newborn's file to the mother's existing patient record so
    // birth/maternal history is reachable from the child's file.
    @ManyToOne
    @JoinColumn(name = "mother_patient_id")
    private Patient motherPatient;

    @ManyToOne
    @JoinColumn(name = "birth_facility_id")
    private Facility birthFacility;

    @Column(name = "birth_weight_grams")
    private Integer birthWeightGrams;

    @Column(name = "birth_length_cm")
    private BigDecimal birthLengthCm;

    @Column(name = "apgar_score_1min")
    private Integer apgarScore1Min;

    @Column(name = "apgar_score_5min")
    private Integer apgarScore5Min;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.uhid == null || this.uhid.isBlank()) {
            this.uhid = generateUhid();
        }
    }

    private static String generateUhid() {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        String randomSuffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "UDHR-" + timestamp + "-" + randomSuffix;
    }
}
