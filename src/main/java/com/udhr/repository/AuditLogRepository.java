package com.udhr.repository;

import com.udhr.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByStaffIdOrderByCreatedAtDesc(Long staffId);
    List<AuditLog> findByPatientIdOrderByCreatedAtDesc(Long patientId);
}
