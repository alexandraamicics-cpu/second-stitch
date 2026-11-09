package com.secondstitch.auth;

import jakarta.validation.constraints.NotBlank;

/** The code from a "Confirm your email" link. */
public record VerifyRequest(@NotBlank String token) {
}
