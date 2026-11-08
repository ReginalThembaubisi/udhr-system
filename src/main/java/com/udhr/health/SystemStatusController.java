package com.udhr.health;

import com.udhr.repository.AuditLogRepository;
import com.udhr.repository.PatientRepository;
import com.udhr.repository.StaffRepository;
import com.udhr.repository.StockItemRepository;
import com.udhr.repository.VisitRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Is the system working, and which database is it using?
 *
 * Where the database path is stored: application.properties reads
 *   spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/udhr_db}
 * so the live address comes from the SPRING_DATASOURCE_URL setting on the
 * host (Render), with a local default for development. The username and
 * password come from SPRING_DATASOURCE_USERNAME / _PASSWORD the same way and
 * are never shown here.
 */
@RestController
public class SystemStatusController {

    private final DatabaseHealth health;
    private final PatientRepository patients;
    private final StaffRepository staff;
    private final VisitRepository visits;
    private final StockItemRepository stock;
    private final AuditLogRepository audit;

    @Value("${spring.datasource.url:}")
    private String databaseUrl;

    public SystemStatusController(DatabaseHealth health, PatientRepository patients, StaffRepository staff,
                                  VisitRepository visits, StockItemRepository stock, AuditLogRepository audit) {
        this.health = health;
        this.patients = patients;
        this.staff = staff;
        this.visits = visits;
        this.stock = stock;
        this.audit = audit;
    }

    /** Open to anyone: only "working or not", nothing about the setup. */
    @GetMapping("/api/system-status")
    public ResponseEntity<?> publicStatus() {
        boolean up = health.checkNow();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", up ? "ok" : "database-unavailable");
        body.put("database", up ? "connected" : "unavailable");
        body.put("checkedAt", LocalDateTime.now().toString());
        if (!up) body.put("message", DatabaseGuardFilter.MESSAGE);
        return ResponseEntity.status(up ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    /** System admin only (SecurityConfig): which database, where it's configured, and how much is in it. */
    @GetMapping("/api/system-admin/system-status")
    public ResponseEntity<?> adminStatus() {
        boolean up = health.checkNow();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("database", up ? "connected" : "unavailable");
        body.put("product", health.product());
        body.put("address", maskedUrl(databaseUrl));
        body.put("configuredIn", "application.properties: spring.datasource.url = ${SPRING_DATASOURCE_URL}, set on the Render dashboard (Environment tab)");
        if (up) {
            Map<String, Long> counts = new LinkedHashMap<>();
            counts.put("patients", patients.count());
            counts.put("staff", staff.count());
            counts.put("visits", visits.count());
            counts.put("stockItems", stock.count());
            counts.put("auditEntries", audit.count());
            body.put("records", counts);
        }
        body.put("checkedAt", LocalDateTime.now().toString());
        return ResponseEntity.ok(body);
    }

    /** jdbc:postgresql://user:secret@host:5432/db?ssl=true becomes jdbc:postgresql://host:5432/db (no password, no options). */
    static String maskedUrl(String url) {
        if (url == null || url.isBlank()) return "not set";
        String masked = url.replaceAll("//[^/@]*@", "//");
        int query = masked.indexOf('?');
        return query > 0 ? masked.substring(0, query) : masked;
    }
}
