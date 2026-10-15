package com.udhr.controller;

import com.udhr.dto.PortalPinRequest;
import com.udhr.dto.AdminPatientLookupResponse;
import com.udhr.dto.CheckInRequest;
import com.udhr.dto.CheckInSummaryResponse;
import com.udhr.model.Patient;
import com.udhr.model.Visit;
import com.udhr.service.CheckInService;
import com.udhr.service.PatientService;
import com.udhr.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/checkin")
public class CheckInController {

    @Autowired
    private CheckInService checkInService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private JwtUtil jwtUtil;

    private String extractStaffNumber(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                return jwtUtil.extractStaffNumber(token);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    @PostMapping
    public ResponseEntity<?> checkIn(@RequestBody CheckInRequest checkInRequest, HttpServletRequest request) {
        String staffNumber = extractStaffNumber(request);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        try {
            Visit visit = checkInService.checkIn(checkInRequest, staffNumber);
            return ResponseEntity.status(HttpStatus.CREATED).body(visit);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/recent")
    public ResponseEntity<?> getRecentCheckIns(HttpServletRequest request) {
        String staffNumber = extractStaffNumber(request);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        try {
            List<CheckInSummaryResponse> visits = checkInService.getTodayCheckIns(staffNumber);
            return ResponseEntity.ok(visits);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // Front desk look-up for a patient who isn't on today's list — non-clinical:
    // demographics and visit history only.
    @GetMapping("/lookup/{identifier}")
    public ResponseEntity<?> lookupPatient(@PathVariable String identifier, HttpServletRequest request) {
        String staffNumber = extractStaffNumber(request);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        try {
            AdminPatientLookupResponse response = checkInService.lookupPatient(identifier);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // Front desk sets or resets a patient's portal PIN after checking ID in person.
    @PostMapping("/portal-pin")
    public ResponseEntity<?> setPortalPin(@RequestBody PortalPinRequest pinRequest, HttpServletRequest request) {
        String staffNumber = extractStaffNumber(request);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        try {
            Patient patient = patientService.setPortalPin(pinRequest.getIdentifier(), pinRequest.getPin(), staffNumber);
            return ResponseEntity.ok(java.util.Map.of(
                    "message", "Portal PIN set for " + patient.getFirstName() + " " + patient.getLastName(),
                    "portalPinSet", true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of("message", e.getMessage()));
        }
    }
}
