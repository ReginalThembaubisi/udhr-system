package com.udhr.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "health_tips")
public class HealthTip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "condition_name", nullable = false)
    private String conditionName; // e.g. "Diabetes", "Hypertension"

    @Column(name = "tip_type", nullable = false)
    private String tipType; // e.g. "LIFESTYLE", "MEDICATION", "GENERAL"

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(nullable = false)
    private String source; // e.g. "South African Department of Health", "WHO"
}
