package com.secondstitch.auth;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Turns the logged-in user's token into their user id.
 * Controllers get the token with @AuthenticationPrincipal Jwt jwt.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
