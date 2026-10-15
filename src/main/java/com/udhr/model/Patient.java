package com.udhr.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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

@Data
@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_number", unique = true)
    private String idNumber;

    @Column(name = "mrn", unique = true, nullable = false)
    private String mrn;

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

    // Next of kin, all optional
    @Column(name = "next_of_kin_first_name")
    private String nextOfKinFirstName;

    @Column(name = "next_of_kin_last_name")
    private String nextOfKinLastName;

    @Column(name = "next_of_kin_relationship")
    private String nextOfKinRelationship;

    @Column(name = "next_of_kin_phone")
    private String nextOfKinPhone;

    // Links a newborn's file to the mother's existing patient record so
    // birth/maternal history is reachable from the child's file. The MRN
    // (not a separate identifier) is what gives the newborn's own file a
    // permanent identity before any ID number exists.
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

    // Patient portal sign-in. An SA ID number already contains the date of
    // birth, so ID + date of birth alone let anyone who knew someone's ID number
    // open their record. The portal now also needs a PIN the front desk sets
    // after checking the patient's identity in person. Only the hash is stored.
    @JsonIgnore
    @Column(name = "portal_pin_hash")
    private String portalPinHash;

    @JsonIgnore
    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts;

    @JsonIgnore
    @Column(name = "login_locked_until")
    private LocalDateTime loginLockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Lets the front desk see whether the patient can sign in to the portal yet,
    // without ever exposing the PIN or its hash.
    @JsonProperty("portalPinSet")
    public boolean isPortalPinSet() {
        return portalPinHash != null;
    }
}
