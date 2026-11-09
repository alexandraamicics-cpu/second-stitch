package com.secondstitch.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "Enter your email") @Email(message = "Enter a valid email") @Size(max = 120) String email,
        // BCrypt only uses the first 72 bytes of a password, so longer ones are refused
        @NotBlank(message = "Choose a password")
        @Size(min = 8, max = 72, message = "Use 8 to 72 characters") String password,
        @Size(max = 40, message = "Keep your name under 40 characters") String name
) {
}
