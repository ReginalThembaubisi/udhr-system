package com.udhr.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "ingredient_warnings")
public class IngredientWarning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(name = "condition_name", nullable = false)
    private String conditionName; // e.g. "Diabetes", "Hypertension", or allergen name like "Nuts", "Penicillin"

    @Column(nullable = false)
    private String severity; // "SAFE", "CAUTION", "DANGER"

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reason;
}
