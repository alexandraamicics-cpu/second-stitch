package com.secondstitch.auth;

import jakarta.servlet.http.HttpServletRequest;

/** Works out the visitor's IP address, used to limit wrong-password attempts. */
public final class ClientAddress {

    private ClientAddress() {
    }

    /**
     * Hosts like Render sit in front of the app and add the visitor's address as the
     * last entry of X-Forwarded-For. Earlier entries can be faked, so use the last one.
     */
    public static String of(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] parts = forwarded.split(",");
            return parts[parts.length - 1].trim();
        }
        return request.getRemoteAddr();
    }
}
