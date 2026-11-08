package com.udhr.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * A browser that has proved itself once (authenticator code for staff, SMS
 * code for patients) and is remembered for 30 days for one account. The
 * browser holds a random token in an HttpOnly cookie; only its SHA-256 hash
 * is stored here, so a database leak can't be used to pose as a device.
 */
@Data
@Entity
@Table(name = "trusted_devices", indexes = @Index(name = "idx_trusted_device_hash", columnList = "tokenHash"))
public class TrustedDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String tokenHash;

    private Long staffId;

    private Long patientId;

    // "Chrome on Windows", shown in Sign-in security so people recognise it.
    @Column(length = 80)
    private String label;

    @Column(length = 64)
    private String ipAddress;

    private LocalDateTime createdAt;

    private LocalDateTime lastUsedAt;

    private LocalDateTime expiresAt;
}
