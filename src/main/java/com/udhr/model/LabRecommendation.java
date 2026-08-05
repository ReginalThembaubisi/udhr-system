package com.udhr.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "lab_recommendations")
public class LabRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "alert_id", nullable = false)
    private ClinicalAlert alert;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "test_name", nullable = false)
    private String testName;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Column(name = "icd_code")
    private String icdCode;

    @Column(name = "is_ordered", nullable = false)
    private Boolean isOrdered = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
