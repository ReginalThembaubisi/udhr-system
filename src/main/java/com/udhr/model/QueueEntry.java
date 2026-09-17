package com.udhr.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "queue_entries")
public class QueueEntry {

    public enum Department {
        GP, DENTAL, MATERNITY, PEDIATRICS, CASUALTY, CHRONIC_CLUB
    }

    public enum Urgency {
        GREEN, YELLOW, RED
    }

    public enum Status {
        WAITING, IN_CONSULTATION, AWAITING_PHARMACY, COMPLETED, CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Department department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Urgency urgency = Urgency.GREEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.WAITING;

    // Resets to 1 each day, per facility — the number called out at reception.
    @Column(name = "queue_number", nullable = false)
    private Integer queueNumber;

    @Column(name = "queue_date", nullable = false)
    private LocalDate queueDate;

    @Column(nullable = false)
    private String reason;

    @ManyToOne
    @JoinColumn(name = "checked_in_by")
    private Staff checkedInBy;

    @ManyToOne
    @JoinColumn(name = "attending_staff_id")
    private Staff attendingStaff;

    @ManyToOne
    @JoinColumn(name = "visit_id")
    private Visit visit;

    @Column(name = "checked_in_at", nullable = false, updatable = false)
    private LocalDateTime checkedInAt;

    @Column(name = "called_at")
    private LocalDateTime calledAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        this.checkedInAt = LocalDateTime.now();
        this.queueDate = LocalDate.now();
    }
}
