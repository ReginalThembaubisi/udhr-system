package com.udhr.controller;

import com.udhr.dto.StaffRequest;
import com.udhr.model.Staff;
import com.udhr.service.StaffService;
import com.udhr.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/staff")
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

        try {
            Staff staff = staffService.registerStaff(staffRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(staff);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivateStaff(@PathVariable Long id, HttpServletRequest request) {
        String staffNumber = extractStaffNumber(request);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        try {
            Staff staff = staffService.deactivateStaff(id);
            return ResponseEntity.ok(staff);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllStaff(HttpServletRequest request) {
        String staffNumber = extractStaffNumber(request);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        try {
            List<Staff> staffList = staffService.getAllStaff();
            return ResponseEntity.ok(staffList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
