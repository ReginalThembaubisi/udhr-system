package com.udhr.service;

import com.udhr.dto.StaffRequest;
import com.udhr.model.Facility;
import com.udhr.model.Staff;
import com.udhr.repository.FacilityRepository;
import com.udhr.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;

@Service
public class StaffService {

    // ADMIN is the clinic's front desk — the person a patient walks up to on
    // arrival. SYSTEM_ADMIN is the separate oversight role that manages accounts
    // and reviews the audit trail; the two are not interchangeable.
    private static final Set<String> VALID_ROLES = Set.of("ADMIN", "DOCTOR", "NURSE", "PHARMACIST", "SYSTEM_ADMIN");

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private AuditService auditService;

    public Staff registerStaff(StaffRequest request) {
        if (staffRepository.existsByStaffNumber(request.getStaffNumber())) {
            throw new RuntimeException("Staff number already exists");
        }

        String role = request.getRole() != null ? request.getRole().toUpperCase() : null;
        if (role == null || !VALID_ROLES.contains(role)) {
            throw new RuntimeException("Role must be one of: " + VALID_ROLES);
        }
        if (isBlank(request.getFirstName()) || isBlank(request.getLastName()) || isBlank(request.getEmail())) {
            throw new RuntimeException("First name, last name and email are required");
        }
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new RuntimeException("Temporary password must be at least 8 characters");
        }

        Facility facility = facilityRepository.findById(request.getFacilityId())
                .orElseThrow(() -> new RuntimeException("Facility not found"));

        Staff staff = new Staff();
        staff.setStaffNumber(request.getStaffNumber());
        staff.setFirstName(request.getFirstName());
        staff.setLastName(request.getLastName());
        staff.setRole(role);
        staff.setFacility(facility);
        staff.setEmail(request.getEmail());
        staff.setPassword(passwordEncoder.encode(request.getPassword()));
        staff.setActive(true);

        Staff saved = staffRepository.save(staff);

        // Creating an account grants someone access to patient data, so who
        // granted it, to whom, and at what level all belong in the trail.
        auditService.recordCreate(auditService.currentStaff(), null, "CREATE_STAFF_ACCOUNT",
                "Created an account for " + saved.getFirstName() + " " + saved.getLastName()
                        + " (" + saved.getStaffNumber() + ")",
                "Role " + saved.getRole() + " at " + (facility.getName() != null ? facility.getName() : "facility #" + facility.getId()));

        return saved;
    }

    public Staff deactivateStaff(Long id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        AdminSafeguards.checkCanDeactivate(staff, auditService.currentStaff(), staffRepository);

        staff.setActive(false);
        Staff saved = staffRepository.save(staff);

        auditService.recordChange(auditService.currentStaff(), null, "DEACTIVATE_STAFF_ACCOUNT",
                "Deactivated the account for " + saved.getFirstName() + " " + saved.getLastName()
                        + " (" + saved.getStaffNumber() + ")",
                "Active", "Inactive");

        return saved;
    }

    public List<Staff> getAllStaff() {
        return staffRepository.findAll();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
