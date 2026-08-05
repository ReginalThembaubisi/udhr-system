package com.udhr.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "differential_diagnoses")
public class DifferentialDiagnosis {

    public enum Likelihood {
        LOW,
        MODERATE,
        HIGH
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "alert_id", nullable = false)
    private ClinicalAlert alert;

    @Column(name = "icd_code", nullable = false)
    private String icdCode;

    @Column(name = "condition_name", nullable = false)
    private String conditionName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Likelihood likelihood;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reasoning;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
