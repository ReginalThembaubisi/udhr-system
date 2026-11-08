package com.udhr.controller;

import com.udhr.dto.StaffRequest;
import com.udhr.model.Patient;
import com.udhr.model.Staff;
import com.udhr.repository.PatientRepository;
import com.udhr.repository.StaffRepository;
import com.udhr.service.AuditService;
import com.udhr.service.StaffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Set;

/**
 * Facility/front-desk administrative tools. Separate from SYSTEM_ADMIN and
 * separate from the clinical record APIs. SecurityConfig restricts the entire
 * route group to authenticated ADMIN users; we check the actor again here.
 */
@RestController
@RequestMapping("/api/hospital-admin")
public class HospitalAdminController {
    private static final Set<String> LOCAL_CLINICAL_ROLES = Set.of("DOCTOR", "NURSE", "PHARMACIST");
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired private PatientRepository patientRepository;
    @Autowired private StaffRepository staffRepository;
    @Autowired private StaffService staffService;
    @Autowired private AuditService auditService;

    private Staff actor() {
        Staff staff = auditService.currentStaff();
        if (staff == null || !"ADMIN".equals(staff.getRole()) || !Boolean.TRUE.equals(staff.getActive())
                || staff.getFacility() == null) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Front Desk account required");
        }
        return staff;
    }

    public record PatientSummary(Long id, String mrn, String firstName, String lastName,
                                 String idNumber, String passportNumber, LocalDate dateOfBirth,
                                 String gender, String contactNumber, boolean portalPinSet) {
        static PatientSummary from(Patient p) {
            return new PatientSummary(p.getId(), p.getMrn(), p.getFirstName(), p.getLastName(),
                    p.getIdNumber(), p.getPassportNumber(), p.getDateOfBirth(), p.getGender(),
                    p.getContactNumber(), p.isPortalPinSet());
        }
    }

    public record PatientDirectory(List<PatientSummary> patients, long total, int page, int pages) { }

    @GetMapping("/patients")
    public PatientDirectory patients(@RequestParam(defaultValue = "") String query,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "15") int size) {
        Staff operator = actor();
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(30, size));
        String term = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        if (term.length() > 120) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Search is too long");
        }
        Specification<Patient> spec = (root, cq, cb) -> {
            if (term.isBlank()) return cb.conjunction();
            String contains = "%" + term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            var matches = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            for (String field : List.of("mrn", "firstName", "lastName", "idNumber", "passportNumber", "contactNumber")) {
                matches.add(cb.like(cb.lower(root.get(field)), contains, '\\'));
            }
            matches.add(cb.like(cb.lower(cb.concat(cb.concat(root.get("firstName"), " "), root.get("lastName"))), contains, '\\'));
            return cb.or(matches.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        Page<Patient> result = patientRepository.findAll(spec,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        auditService.recordView(operator, null, "VIEW_PATIENT_DIRECTORY",
                "Front Desk viewed the registered-patient directory at " + operator.getFacility().getName()
                        + " (search results: " + result.getTotalElements() + ", page: " + safePage + ")");
        return new PatientDirectory(result.getContent().stream().map(PatientSummary::from).toList(),
                result.getTotalElements(), result.getNumber(), result.getTotalPages());
    }

    public record StaffSummary(Long id, String staffNumber, String firstName, String lastName,
                               String role, String email, boolean active) {
        static StaffSummary from(Staff s) {
            return new StaffSummary(s.getId(), s.getStaffNumber(), s.getFirstName(), s.getLastName(),
                    s.getRole(), s.getEmail(), Boolean.TRUE.equals(s.getActive()));
        }
    }

    @GetMapping("/staff")
    public List<StaffSummary> staff() {
        Staff operator = actor();
        List<StaffSummary> result = staffRepository.findByFacilityId(operator.getFacility().getId()).stream()
                .filter(s -> !"SYSTEM_ADMIN".equals(s.getRole())).map(StaffSummary::from).toList();
        auditService.recordView(operator, null, "VIEW_FACILITY_STAFF",
                "Front Desk viewed staff accounts at " + operator.getFacility().getName());
        return result;
    }

    public record NewStaffAccount(StaffSummary staff, String temporaryPassword) { }

    @PostMapping("/staff")
    public ResponseEntity<NewStaffAccount> createLocalClinicalStaff(@RequestBody StaffRequest request) {
        Staff operator = actor();
        String role = request.getRole() == null ? "" : request.getRole().trim().toUpperCase(java.util.Locale.ROOT);
        if (!LOCAL_CLINICAL_ROLES.contains(role)) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Front Desk may create only doctor, nurse or pharmacist accounts");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()
                || request.getStaffNumber() == null || request.getStaffNumber().isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Staff number and email are required");
        }
        // Never trust a facility ID or privileged role supplied by the browser.
        request.setRole(role);
        request.setFacilityId(operator.getFacility().getId());
        byte[] randomBytes = new byte[18];
        RANDOM.nextBytes(randomBytes);
        String temporaryPassword = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        request.setPassword(temporaryPassword);
        Staff added = staffService.registerStaff(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Cache-Control", "no-store")
                .body(new NewStaffAccount(StaffSummary.from(added), temporaryPassword));
    }
}
