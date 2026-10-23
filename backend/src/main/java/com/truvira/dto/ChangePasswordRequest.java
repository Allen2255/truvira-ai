package com.truvira.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body for PUT /api/auth/password:  {"currentPassword": "...", "newPassword": "..."}
 * We ask for the CURRENT password too, so someone who finds your laptop already logged in
 * still can't change your password (and lock you out).
 */
public record ChangePasswordRequest(
        @NotBlank(message = "Enter your current password")
        String currentPassword,

        // At least 8 characters. (Max 72 because BCrypt only uses the first 72 bytes.)
        @NotBlank(message = "Enter a new password")
        @Size(min = 8, max = 72, message = "New password must be 8 to 72 characters")
        String newPassword
) {
}
