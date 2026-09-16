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

@Service
public class StaffService {

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public Staff registerStaff(StaffRequest request) {
        if (staffRepository.existsByStaffNumber(request.getStaffNumber())) {
            throw new RuntimeException("Staff number already exists");
        }

        Facility facility = facilityRepository.findById(request.getFacilityId())
                .orElseThrow(() -> new RuntimeException("Facility not found"));

        Staff staff = new Staff();
        staff.setStaffNumber(request.getStaffNumber());
        staff.setFirstName(request.getFirstName());
        staff.setLastName(request.getLastName());
        staff.setRole(request.getRole());
        staff.setFacility(facility);
        staff.setEmail(request.getEmail());
        staff.setPassword(passwordEncoder.encode(request.getPassword()));
        staff.setActive(true);
        staff.setMustChangePassword(true); // admin-set password is temporary until the new hire changes it

        return staffRepository.save(staff);
    }

    public Staff deactivateStaff(Long id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        staff.setActive(false);
        return staffRepository.save(staff);
    }

    public List<Staff> getAllStaff() {
        return staffRepository.findAll();
    }
}
