package com.udhr.controller;

import com.udhr.dto.ReferralReportResponse;
import com.udhr.dto.ReferralRequest;
import com.udhr.dto.ReferralResponseRequest;
import com.udhr.model.Referral;
import com.udhr.service.ReferralService;
import com.udhr.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/referrals")
public class ReferralController {

    @Autowired
    private ReferralService referralService;

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
    public ResponseEntity<?> createReferral(@RequestBody ReferralRequest referralRequest, HttpServletRequest request) {
        String staffNumber = extractStaffNumber(request);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        try {
            Referral referral = referralService.createReferral(referralRequest, staffNumber);
            return ResponseEntity.status(HttpStatus.CREATED).body(referral);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/incoming")
    public ResponseEntity<?> getIncoming(HttpServletRequest request) {
        String staffNumber = extractStaffNumber(request);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        try {
            List<Referral> referrals = referralService.getIncoming(staffNumber);
            return ResponseEntity.ok(referrals);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/outgoing")
    public ResponseEntity<?> getOutgoing(HttpServletRequest request) {
        String staffNumber = extractStaffNumber(request);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        try {
            List<Referral> referrals = referralService.getOutgoing(staffNumber);
            return ResponseEntity.ok(referrals);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/{id}/respond")
    public ResponseEntity<?> respond(@PathVariable Long id, @RequestBody ReferralResponseRequest responseRequest, HttpServletRequest request) {
        String staffNumber = extractStaffNumber(request);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        try {
            Referral referral = referralService.respond(id, responseRequest, staffNumber);
            return ResponseEntity.ok(referral);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<?> getPatientHistory(@PathVariable Long patientId, HttpServletRequest request) {
        String staffNumber = extractStaffNumber(request);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        try {
            List<Referral> history = referralService.getPatientHistory(patientId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/report")
    public ResponseEntity<?> getReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletRequest request) {
        String staffNumber = extractStaffNumber(request);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        try {
            ReferralReportResponse report = referralService.getReport(staffNumber, startDate, endDate);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/report/export")
    public ResponseEntity<?> exportReportCsv(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletRequest request) {
        String staffNumber = extractStaffNumber(request);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        try {
            String csv = referralService.exportCsv(staffNumber, startDate, endDate);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"referrals.csv\"")
                    .body(csv.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of("message", e.getMessage()));
        }
    }
}
