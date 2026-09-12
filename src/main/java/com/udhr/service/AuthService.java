package com.udhr.service;

import com.udhr.dto.LoginRequest;
import com.udhr.dto.LoginResponse;
import com.udhr.exception.AuthenticationFailedException;
import com.udhr.model.Staff;
import com.udhr.repository.StaffRepository;
import com.udhr.security.JwtUtil;
import com.udhr.security.LoginAttemptService;
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

    @Autowired
    private LoginAttemptService loginAttemptService;

    public LoginResponse login(LoginRequest request) {
        String attemptKey = "staff:" + request.getStaffNumber();
        loginAttemptService.assertNotLocked(attemptKey);

        Staff staff = staffRepository.findByStaffNumber(request.getStaffNumber()).orElse(null);

        // One generic failure message for "no such staff number", "inactive
        // account", and "wrong password" alike -- distinguishing them lets an
        // attacker enumerate valid staff numbers before guessing passwords.
        boolean valid = staff != null
                && Boolean.TRUE.equals(staff.getActive())
                && passwordEncoder.matches(request.getPassword(), staff.getPassword());

        if (!valid) {
            loginAttemptService.recordFailure(attemptKey);
            throw new AuthenticationFailedException("Invalid staff number or password");
        }
        loginAttemptService.recordSuccess(attemptKey);

        String token = jwtUtil.generateToken(staff.getStaffNumber(), staff.getRole());
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
                facilityId
        );
    }
}
