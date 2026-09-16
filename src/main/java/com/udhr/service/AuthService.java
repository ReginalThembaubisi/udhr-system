package com.udhr.service;

import com.udhr.dto.ChangePasswordRequest;
import com.udhr.dto.LoginRequest;
import com.udhr.dto.LoginResponse;
import com.udhr.model.Staff;
import com.udhr.repository.StaffRepository;
import com.udhr.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {
        Staff staff = staffRepository.findByStaffNumber(request.getStaffNumber())
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        if (staff.getActive() == null || !staff.getActive()) {
            throw new RuntimeException("Account is inactive");
        }

        if (!passwordEncoder.matches(request.getPassword(), staff.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        boolean mustChangePassword = staff.getMustChangePassword() != null && staff.getMustChangePassword();
        String token = jwtUtil.generateToken(staff.getStaffNumber(), staff.getRole(), mustChangePassword);
        String fullName = staff.getFirstName() + " " + staff.getLastName();

        Long facilityId = null;
        if (staff.getFacility() != null) {
            facilityId = staff.getFacility().getId();
        }

        return new LoginResponse(
                token,
                staff.getStaffNumber(),
                fullName,
                staff.getRole(),
                facilityId,
                mustChangePassword
        );
    }

    // Returns a freshly issued token (mustChangePassword cleared) because the
    // old token's claim is baked in and immutable — the caller must swap to
    // this one or every subsequent request will keep getting blocked by the
    // filter even though the password was actually changed.
    public LoginResponse changePassword(String staffNumber, ChangePasswordRequest request) {
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), staff.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        if (request.getNewPassword() == null || request.getNewPassword().length() < 8) {
            throw new RuntimeException("New password must be at least 8 characters");
        }

        staff.setPassword(passwordEncoder.encode(request.getNewPassword()));
        staff.setMustChangePassword(false);
        staffRepository.save(staff);

        String token = jwtUtil.generateToken(staff.getStaffNumber(), staff.getRole(), false);
        String fullName = staff.getFirstName() + " " + staff.getLastName();
        Long facilityId = staff.getFacility() != null ? staff.getFacility().getId() : null;

        return new LoginResponse(token, staff.getStaffNumber(), fullName, staff.getRole(), facilityId, false);
    }
}
