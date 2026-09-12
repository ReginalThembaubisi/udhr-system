package com.udhr.controller;

import com.udhr.dto.StaffRequest;
import com.udhr.model.Staff;
import com.udhr.service.StaffService;
import com.udhr.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

// Method-level checks below are a deliberate second line of defense on top
// of SecurityConfig's URL-pattern matcher for /api/staff/** (hasRole ADMIN) --
// staff management (including creating new accounts) must never depend on a
// single, easily-mis-edited URL pattern staying correct.
@RestController
@RequestMapping("/api/staff")
@PreAuthorize("hasRole('ADMIN')")
public class StaffController {

    @Autowired
    private StaffService staffService;

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
    public ResponseEntity<?> registerStaff(@RequestBody StaffRequest staffRequest, HttpServletRequest request) {
        String staffNumber = extractStaffNumber(request);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        Staff staff = staffService.registerStaff(staffRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(staff);
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivateStaff(@PathVariable Long id, HttpServletRequest request) {
        String staffNumber = extractStaffNumber(request);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        Staff staff = staffService.deactivateStaff(id);
        return ResponseEntity.ok(staff);
    }

    @GetMapping
    public ResponseEntity<?> getAllStaff(HttpServletRequest request) {
        String staffNumber = extractStaffNumber(request);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        List<Staff> staffList = staffService.getAllStaff();
        return ResponseEntity.ok(staffList);
    }
}
