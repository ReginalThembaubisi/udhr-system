package com.udhr.repository;

import com.udhr.model.TrustedDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TrustedDeviceRepository extends JpaRepository<TrustedDevice, Long> {
    Optional<TrustedDevice> findFirstByTokenHashAndStaffIdAndExpiresAtAfter(String tokenHash, Long staffId, LocalDateTime now);
    Optional<TrustedDevice> findFirstByTokenHashAndPatientIdAndExpiresAtAfter(String tokenHash, Long patientId, LocalDateTime now);
    List<TrustedDevice> findByStaffIdAndExpiresAtAfterOrderByLastUsedAtDesc(Long staffId, LocalDateTime now);

    @Modifying
    @Transactional
    long deleteByStaffId(Long staffId);

    @Modifying
    @Transactional
    long deleteByPatientId(Long patientId);
}
