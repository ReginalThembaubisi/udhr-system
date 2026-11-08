package com.udhr.controller;

import com.udhr.dto.PharmacyLookupResponse;
import com.udhr.model.Prescription;
import com.udhr.service.PharmacyService;
import com.udhr.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/pharmacy")
public class PharmacyController {

    @Autowired
    private PharmacyService pharmacyService;

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

    // The pharmacist's automatic dispense list — patients the doctor has
    // just finished with, no ID number needed to find them.
    @GetMapping("/queue")
    public ResponseEntity<?> getQueue(HttpServletRequest request) {
        String staffNumber = extractStaffNumber(request);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        try {
            List<PharmacyLookupResponse> queue = pharmacyService.getQueue(staffNumber);
            return ResponseEntity.ok(queue);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/patient/{idNumber}")
    public ResponseEntity<?> lookupPatient(@PathVariable String idNumber, HttpServletRequest request) {
        String staffNumber = extractStaffNumber(request);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        try {
            PharmacyLookupResponse response = pharmacyService.lookup(idNumber, staffNumber);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/dispense/{prescriptionId}")
    public ResponseEntity<?> dispense(@PathVariable Long prescriptionId,
                                      @RequestBody(required = false) java.util.Map<String, String> body,
                                      HttpServletRequest request) {
        String staffNumber = extractStaffNumber(request);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        try {
            // An optional typed quantity ("12 tablets"); without one, the
            // quantity is worked out from the prescription.
            String quantity = body == null ? null : body.get("quantityDispensed");
            PharmacyService.Dispensed result = pharmacyService.dispense(prescriptionId, staffNumber, quantity, null, null);
            java.util.Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("prescription", result.prescription());
            response.put("quantityDispensed", result.dispense().getQuantityDispensed());
            if (result.stockItem() != null) {
                response.put("stockItem", result.stockItem().getMedicationName());
                response.put("stockLeft", result.stockItem().getQuantityOnHand() + " " + result.stockItem().getUnit());
            }
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(java.util.Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of("message", e.getMessage()));
        }
    }
}
