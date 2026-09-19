package com.udhr.repository;

import com.udhr.model.Referral;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReferralRepository extends JpaRepository<Referral, Long> {
    List<Referral> findByPatientIdOrderByReferredAtDesc(Long patientId);
    List<Referral> findByToFacilityIdOrderByReferredAtDesc(Long facilityId);
    List<Referral> findByFromFacilityIdOrderByReferredAtDesc(Long facilityId);
}
