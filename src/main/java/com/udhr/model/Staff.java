package com.udhr.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "staff")
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "staff_number", unique = true, nullable = false)
    private String staffNumber;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String role; // "ADMIN", "DOCTOR", "NURSE"

    @ManyToOne
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Column(unique = true, nullable = false)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    // Optional: only needed for staff to receive SMS alerts (e.g. reorder notifications).
    @Column(name = "contact_number")
    private String contactNumber;

    @Column(nullable = false)
    private Boolean active = true;

    // New staff are created with a temporary password and must set their own
    // before they can use the rest of the system. Left nullable (rather than
    // NOT NULL) so this column can be added to a table that already has rows
    // — those existing accounts get NULL, which is treated as "not required".
    @Column(name = "must_change_password")
    private Boolean mustChangePassword = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
