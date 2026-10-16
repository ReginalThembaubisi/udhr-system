package com.udhr.service;

import com.udhr.dto.FacilityRequest;
import com.udhr.model.Facility;
import com.udhr.repository.FacilityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;

@Service
public class FacilityService {

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private AuditService auditService;

    private static final Set<String> TYPES = Set.of("CLINIC", "HOSPITAL");

    /**
     * Adds a facility to the network. Every doctor and nurse sees it in their
     * referral list, so it is the system administrator's job (SecurityConfig),
     * a duplicate name is refused, and the addition is audited.
     */
    public Facility addFacility(FacilityRequest request) {
        String name = request.getName() == null ? "" : request.getName().trim();
        if (name.isEmpty()) {
            throw new RuntimeException("Facility name is required");
        }
        if (facilityRepository.existsByNameIgnoreCase(name)) {
            throw new RuntimeException("A facility called " + name + " already exists");
        }
        if (request.getProvince() == null || request.getProvince().isBlank()
                || request.getAddress() == null || request.getAddress().isBlank()) {
            throw new RuntimeException("Province and address are required");
        }
        String type = request.getType() == null ? "" : request.getType().trim().toUpperCase();
        if (!TYPES.contains(type)) {
            throw new RuntimeException("Type must be Clinic or Hospital");
        }

        Facility facility = new Facility();
        facility.setName(name);
        facility.setType(type);
        facility.setProvince(request.getProvince());
        facility.setAddress(request.getAddress());
        Facility saved = facilityRepository.save(facility);

        auditService.recordCreate(auditService.currentStaff(), null, "CREATE_FACILITY",
                "Added facility " + saved.getName(), type + (saved.getProvince() != null ? ", " + saved.getProvince() : ""));
        return saved;
    }

    public List<Facility> getAllFacilities() {
        return facilityRepository.findAll();
    }
}
