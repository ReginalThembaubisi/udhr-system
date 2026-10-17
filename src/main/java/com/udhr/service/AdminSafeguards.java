package com.udhr.service;

import com.udhr.model.Staff;
import com.udhr.repository.StaffRepository;

/**
 * Rules that stop the system being left with nobody able to manage it: an
 * account can't switch itself off, and the last active system administrator
 * can't be switched off at all. Recovering a lost system administrator account
 * is done from Render instead (EmergencyRecovery).
 */
final class AdminSafeguards {

    private AdminSafeguards() {}

    static void checkCanDeactivate(Staff target, Staff performedBy, StaffRepository staffRepository) {
        if (performedBy != null && performedBy.getId() != null && performedBy.getId().equals(target.getId())) {
            throw new RuntimeException("You can't deactivate your own account.");
        }
        if ("SYSTEM_ADMIN".equals(target.getRole()) && Boolean.TRUE.equals(target.getActive())) {
            long otherActiveAdmins = staffRepository.findByRole("SYSTEM_ADMIN").stream()
                    .filter(s -> Boolean.TRUE.equals(s.getActive()) && !s.getId().equals(target.getId()))
                    .count();
            if (otherActiveAdmins == 0) {
                throw new RuntimeException("This is the only active system administrator. Create and activate another one first.");
            }
        }
    }
}
