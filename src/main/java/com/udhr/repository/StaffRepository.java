package com.udhr.repository;

import com.udhr.model.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {
    Optional<Staff> findByStaffNumber(String staffNumber);
    List<Staff> findByFacilityId(Long facilityId);
    List<Staff> findByRole(String role);
    boolean existsByStaffNumber(String staffNumber);
}
