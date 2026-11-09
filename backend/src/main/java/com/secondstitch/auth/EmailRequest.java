package com.secondstitch.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Just an email: for "send the confirmation link again" and "forgot password". */
public record EmailRequest(
        @NotBlank(message = "Enter your email") @Email(message = "Enter a valid email") String email
) {
}
