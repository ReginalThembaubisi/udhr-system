package com.udhr.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "symptom_checks")
public class SymptomCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "urgency_level", nullable = false)
    private String urgencyLevel; // "GREEN", "YELLOW", "RED"

    @Column(columnDefinition = "TEXT")
    private String recommendation;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;

    @Column(name = "api_response_dump", columnDefinition = "TEXT")
    private String apiResponseDump;

    @OneToMany(mappedBy = "symptomCheck", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<SymptomCheckDetail> details;

    @PrePersist
    protected void onCreate() {
        this.checkedAt = LocalDateTime.now();
    }
}
