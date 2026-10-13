package com.udhr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The one and only time a reset password exists in readable form — returned to
 * the administrator to hand over, never stored unhashed and never logged.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PasswordResetResponse {
    private String staffNumber;
    private String fullName;
    private String temporaryPassword;
    private String instructions;
}
