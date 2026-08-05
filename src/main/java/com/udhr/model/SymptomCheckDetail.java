package com.udhr.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "symptom_check_details")
public class SymptomCheckDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "symptom_check_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonBackReference
    private SymptomCheck symptomCheck;

    @ManyToOne
    @JoinColumn(name = "symptom_id", nullable = false)
    private Symptom symptom;

    @Column(name = "choice_id", nullable = false)
    private String choiceId; // "present", "absent", "unknown"
}
