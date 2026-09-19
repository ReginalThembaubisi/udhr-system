package com.udhr.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "referrals")
public class Referral {

    public enum Urgency {
        ROUTINE, URGENT, EMERGENCY
    }

    public enum Status {
        PENDING, ACCEPTED, DECLINED, COMPLETED, CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "from_facility_id", nullable = false)
    private Facility fromFacility;

    @ManyToOne
    @JoinColumn(name = "to_facility_id", nullable = false)
    private Facility toFacility;

    @ManyToOne
    @JoinColumn(name = "referred_by", nullable = false)
    private Staff referredBy;

    @ManyToOne
    @JoinColumn(name = "visit_id")
    private Visit visit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Urgency urgency = Urgency.ROUTINE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(nullable = false)
    private String reason;

    // What's been done so far — diagnosis, meds, vitals trend — so the
    // receiving facility isn't starting from zero.
    @Column(name = "clinical_summary", columnDefinition = "TEXT")
    private String clinicalSummary;

    @ManyToOne
    @JoinColumn(name = "responded_by")
    private Staff respondedBy;

    @Column(name = "response_notes", columnDefinition = "TEXT")
    private String responseNotes;

    @Column(name = "referred_at", nullable = false, updatable = false)
    private LocalDateTime referredAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @PrePersist
    protected void onCreate() {
        this.referredAt = LocalDateTime.now();
    }
}
