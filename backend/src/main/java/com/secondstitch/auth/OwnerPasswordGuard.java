package com.secondstitch.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Set;

/**
 * Anyone can look (GET), but only the owner can change things (POST, PUT, DELETE).
 *
 * The frontend sends the password in an "X-Owner-Password" header. It travels over
 * HTTPS, so it's encrypted on the way. If no ADMIN_PASSWORD is set (e.g. on your
 * laptop), everything stays open so local development is easy.
 */
@Component
public class OwnerPasswordGuard implements HandlerInterceptor {

    public static final String HEADER = "X-Owner-Password";

    private static final Logger log = LoggerFactory.getLogger(OwnerPasswordGuard.class);
    private static final Set<String> CHANGES = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final byte[] expectedHash;
    private final FailedAttempts failedAttempts;
    private final ObjectMapper json;

    public OwnerPasswordGuard(@Value("${app.admin-password:}") String password,
                              FailedAttempts failedAttempts,
                              ObjectMapper json) {
        this.expectedHash = password.isBlank() ? null : sha256(password);
        this.failedAttempts = failedAttempts;
        this.json = json;
        if (expectedHash == null) {
            log.warn("No ADMIN_PASSWORD set: anyone can change data. Fine locally, not for a live site.");
        }
    }

    /** True when a password is required to make changes. */
    public boolean isLocked() {
        return expectedHash != null;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!isLocked() || !CHANGES.contains(request.getMethod())) {
            return true; // looking is always allowed
        }

        String who = clientAddress(request);
        if (failedAttempts.tooMany(who)) {
            return reject(response, HttpStatus.TOO_MANY_REQUESTS,
                    "Too many wrong passwords. Try again in 15 minutes.");
        }

        String given = request.getHeader(HEADER);
        if (given != null && matches(given)) {
            failedAttempts.clear(who);
            return true;
        }

        if (given != null) {
            failedAttempts.record(who);
            return reject(response, HttpStatus.UNAUTHORIZED, "That password isn't right");
        }
        return reject(response, HttpStatus.UNAUTHORIZED, "Unlock editing to make changes");
    }

    /**
     * Compares hashes with MessageDigest.isEqual, which takes the same time
     * however many characters match, so timing can't leak the password.
     */
    private boolean matches(String given) {
        return MessageDigest.isEqual(sha256(given), expectedHash);
    }

    /**
     * Hosts like Render sit in front of the app and add the visitor's address as the
     * last entry of X-Forwarded-For. Earlier entries can be faked by the visitor, so use the last.
     */
    private static String clientAddress(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] parts = forwarded.split(",");
            return parts[parts.length - 1].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean reject(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        json.writeValue(response.getWriter(), Map.of("message", message, "fields", Map.of()));
        return false;
    }

    private static byte[] sha256(String text) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
