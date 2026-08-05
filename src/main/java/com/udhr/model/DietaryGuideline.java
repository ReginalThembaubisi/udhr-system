package com.udhr.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "dietary_guidelines")
public class DietaryGuideline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "condition_name", nullable = false)
    private String conditionName; // e.g. "Diabetes", "Hypertension"

    @Column(name = "food_type", nullable = false)
    private String foodType; // "EAT", "AVOID"

    @Column(name = "food_item", nullable = false)
    private String foodItem;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String source;
}
