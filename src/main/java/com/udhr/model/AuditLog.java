package com.udhr.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nullable: a patient viewing their own record (PatientPortalService) logs
    // with no staff actor at all, not just an unresolved one.
    @ManyToOne
    @JoinColumn(name = "staff_id", nullable = true)
    private Staff staff;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = true)
    private Patient patient;

    @Column(nullable = false)
    private String action;

    @Column(columnDefinition = "TEXT")
    private String description;

    // What the record held before this change, and what it holds after. The
    // audit trail has to answer "what was changed", not only "something was
    // changed" — without these, an administrator investigating an incident can
    // see that a record was edited but not what the edit did.
    //
    // Null on actions that only read a record: a view changes nothing. Stored as
    // readable text rather than a full snapshot, so reviewing the trail doesn't
    // become a second copy of the clinical record.
    @Column(name = "previous_value", columnDefinition = "TEXT")
    private String previousValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
