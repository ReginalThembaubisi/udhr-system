package com.udhr.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import com.udhr.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<AuditLog> {
    List<AuditLog> findByStaffIdOrderByCreatedAtDesc(Long staffId);
    List<AuditLog> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    // The whole trail, newest first — the system administrator's starting point
    // when investigating an incident rather than one known patient or staff member.
    List<AuditLog> findAllByOrderByCreatedAtDesc();

    // For the "last sign-in" notice shown to staff after signing in.
    Optional<AuditLog> findFirstByStaffIdAndActionOrderByCreatedAtDesc(Long staffId, String action);

    long countByStaffIdAndActionInAndCreatedAtAfter(Long staffId, Collection<String> actions, LocalDateTime after);

    // A reason given to open a patient outside today's list covers them for a while.
    boolean existsByStaffIdAndPatientIdAndActionAndCreatedAtAfter(Long staffId, Long patientId, String action, LocalDateTime after);

    // The security alerts read recent entries of these kinds.
    List<AuditLog> findByActionInAndCreatedAtAfterOrderByCreatedAtAsc(Collection<String> actions, LocalDateTime after);
}
