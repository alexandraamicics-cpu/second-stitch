package com.secondstitch.auth;

/** Sent back after signing up or logging in. The frontend keeps the token and sends it with every request. */
public record AuthResponse(String token, UserResponse user) {
}
