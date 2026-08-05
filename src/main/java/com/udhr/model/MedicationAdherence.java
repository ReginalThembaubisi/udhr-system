package com.udhr.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "medication_adherence")
public class MedicationAdherence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "reminder_id", nullable = false)
    private MedicationReminder reminder;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "scheduled_time", nullable = false)
    private LocalDateTime scheduledTime;

    @Column(name = "taken_at")
    private LocalDateTime takenAt;

    @Column(nullable = false)
    private String status = "PENDING"; // "PENDING", "TAKEN", "MISSED"

    @Column(columnDefinition = "TEXT")
    private String notes;
}
