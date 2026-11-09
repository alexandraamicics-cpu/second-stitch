package com.secondstitch.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The code from a "Reset your password" link, plus the new password. */
public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank(message = "Choose a password")
        @Size(min = 8, max = 72, message = "Use 8 to 72 characters") String password
) {
}
