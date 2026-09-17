package com.udhr.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "vitals")
public class Vitals {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "queue_entry_id")
    private QueueEntry queueEntry;

    @ManyToOne
    @JoinColumn(name = "visit_id")
    private Visit visit;

    @ManyToOne
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @ManyToOne
    @JoinColumn(name = "recorded_by", nullable = false)
    private Staff recordedBy;

    @Column(name = "systolic_bp")
    private Integer systolicBp;

    @Column(name = "diastolic_bp")
    private Integer diastolicBp;

    @Column(name = "temperature_c")
    private BigDecimal temperatureC;

    @Column(name = "pulse_bpm")
    private Integer pulseBpm;

    @Column(name = "respiratory_rate")
    private Integer respiratoryRate;

    @Column(name = "oxygen_saturation")
    private BigDecimal oxygenSaturation;

    @Column(name = "weight_kg")
    private BigDecimal weightKg;

    @Column(name = "height_cm")
    private BigDecimal heightCm;

    private BigDecimal bmi;

    @Column(name = "glucose_mmol")
    private BigDecimal glucoseMmol;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    protected void onCreate() {
        this.recordedAt = LocalDateTime.now();
        if (this.weightKg != null && this.heightCm != null && this.heightCm.doubleValue() > 0) {
            double heightM = this.heightCm.doubleValue() / 100.0;
            double bmiValue = this.weightKg.doubleValue() / (heightM * heightM);
            this.bmi = BigDecimal.valueOf(Math.round(bmiValue * 10.0) / 10.0);
        }
    }
}
