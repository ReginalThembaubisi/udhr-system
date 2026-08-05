package com.udhr.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "clinical_alerts")
public class ClinicalAlert {

    public enum AlertType {
        NON_RESPONSE,
        DRUG_FOOD,
        ALTERNATIVE_DIAGNOSIS,
        LAB_REQUIRED,
        MANUAL
    }

    public enum AlertSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "doctor_id")
    private Staff doctor;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertSeverity severity;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "is_resolved", nullable = false)
    private Boolean isResolved = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
