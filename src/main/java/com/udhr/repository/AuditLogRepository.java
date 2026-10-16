package com.udhr.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import com.udhr.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByStaffIdOrderByCreatedAtDesc(Long staffId);
    List<AuditLog> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    // The whole trail, newest first — the system administrator's starting point
    // when investigating an incident rather than one known patient or staff member.
    List<AuditLog> findAllByOrderByCreatedAtDesc();

    // For the "last sign-in" notice shown to staff after signing in.
    Optional<AuditLog> findFirstByStaffIdAndActionOrderByCreatedAtDesc(Long staffId, String action);

    long countByStaffIdAndActionInAndCreatedAtAfter(Long staffId, Collection<String> actions, LocalDateTime after);
}
