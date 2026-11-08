package com.udhr.service;

import com.udhr.model.Patient;
import com.udhr.model.Staff;
import com.udhr.model.TrustedDevice;
import com.udhr.repository.TrustedDeviceRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

/**
 * "Remember this device": after someone proves a new browser once (the
 * authenticator code for staff, a code texted to the cellphone for
 * patients), that browser skips the extra step for 30 days for that account.
 * A new or unknown device always gets the extra step.
 *
 * Forgotten whenever the account's sessions are ended: a password change or
 * reset, a new PIN, "End all sessions" by the system admin, or a reset of the
 * authenticator app. So a stolen laptop stops being trusted as soon as the
 * owner changes their password.
 */
@Service
public class TrustedDeviceService {

    public static final String COOKIE = "udhr_device";
    public static final int DAYS = 30;

    private final TrustedDeviceRepository repository;
    private final SecureRandom random = new SecureRandom();

    public TrustedDeviceService(TrustedDeviceRepository repository) {
        this.repository = repository;
    }

    public boolean isTrusted(String cookieValue, Staff staff) {
        if (cookieValue == null || cookieValue.isBlank() || staff == null) return false;
        return repository.findFirstByTokenHashAndStaffIdAndExpiresAtAfter(hash(cookieValue), staff.getId(), LocalDateTime.now())
                .map(this::touch).isPresent();
    }

    public boolean isTrusted(String cookieValue, Patient patient) {
        if (cookieValue == null || cookieValue.isBlank() || patient == null) return false;
        return repository.findFirstByTokenHashAndPatientIdAndExpiresAtAfter(hash(cookieValue), patient.getId(), LocalDateTime.now())
                .map(this::touch).isPresent();
    }

    /** Remembers this browser for the account; returns the cookie value to set (the existing one if the browser has one). */
    public String trust(String existingCookie, Staff staff, String userAgent, String ip) {
        String value = cookieValue(existingCookie);
        TrustedDevice device = newDevice(value, userAgent, ip);
        device.setStaffId(staff.getId());
        repository.save(device);
        return value;
    }

    public String trust(String existingCookie, Patient patient, String userAgent, String ip) {
        String value = cookieValue(existingCookie);
        TrustedDevice device = newDevice(value, userAgent, ip);
        device.setPatientId(patient.getId());
        repository.save(device);
        return value;
    }

    public List<TrustedDevice> devicesOf(Staff staff) {
        return repository.findByStaffIdAndExpiresAtAfterOrderByLastUsedAtDesc(staff.getId(), LocalDateTime.now());
    }

    public void forgetAll(Staff staff) {
        if (staff != null && staff.getId() != null) repository.deleteByStaffId(staff.getId());
    }

    public void forgetAll(Patient patient) {
        if (patient != null && patient.getId() != null) repository.deleteByPatientId(patient.getId());
    }

    private TrustedDevice touch(TrustedDevice device) {
        device.setLastUsedAt(LocalDateTime.now());
        return repository.save(device);
    }

    private TrustedDevice newDevice(String value, String userAgent, String ip) {
        LocalDateTime now = LocalDateTime.now();
        TrustedDevice device = new TrustedDevice();
        device.setTokenHash(hash(value));
        device.setLabel(label(userAgent));
        device.setIpAddress(ip);
        device.setCreatedAt(now);
        device.setLastUsedAt(now);
        device.setExpiresAt(now.plusDays(DAYS));
        return device;
    }

    // Reuse the browser's token if it has a sensible one, so one browser shared
    // by two accounts (a clinic PC) keeps one cookie.
    private String cookieValue(String existing) {
        if (existing != null && existing.matches("[A-Za-z0-9_-]{40,64}")) return existing;
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** "Chrome on Windows" from a browser's User-Agent; nothing more specific is kept. */
    static String label(String ua) {
        if (ua == null) return "Unknown browser";
        String browser = ua.contains("Edg/") ? "Edge" : ua.contains("OPR/") ? "Opera" : ua.contains("Chrome/") ? "Chrome"
                : ua.contains("Firefox/") ? "Firefox" : ua.contains("Safari/") ? "Safari" : "Browser";
        String os = ua.contains("Android") ? "Android phone" : ua.contains("iPhone") ? "iPhone" : ua.contains("iPad") ? "iPad"
                : ua.contains("Windows") ? "Windows" : ua.contains("Mac OS") ? "Mac" : ua.contains("Linux") ? "Linux" : "unknown system";
        return browser + " on " + os;
    }
}
