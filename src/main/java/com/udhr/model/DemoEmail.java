package com.udhr.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * An email or text message the system would have sent, kept here instead
 * because no email or SMS service is set up (the school demo). Shown in the demo inbox on the system
 * admin's Security alerts page.
 */
@Data
@Entity
@Table(name = "demo_emails")
public class DemoEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 1000)
    private String recipients;

    @Column(length = 300)
    private String subject;

    @Column(length = 4000)
    private String body;

    private LocalDateTime sentAt;

    // "SMS" for a text message (PIN reset codes); null or "EMAIL" for an email.
    @Column(length = 10)
    private String kind;

    @PrePersist
    void stamp() {
        if (sentAt == null) sentAt = LocalDateTime.now();
    }
}
