package com.udhr.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "symptoms")
public class Symptom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "infermedica_id", nullable = false, unique = true)
    private String infermedicaId;

    @Column(nullable = false)
    private String category;

    @Column(name = "icd10_code")
    private String icd10Code;

    @Column(nullable = false)
    private String source;
}
