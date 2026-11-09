package com.secondstitch.auth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 *   GET  /api/auth        { "locked": true }  does this site need a password to make changes?
 *   POST /api/auth/check  { "ok": true }      checks the X-Owner-Password header (401 if wrong)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final OwnerPasswordGuard guard;

    public AuthController(OwnerPasswordGuard guard) {
        this.guard = guard;
    }

    @GetMapping
    public Map<String, Boolean> status() {
        return Map.of("locked", guard.isLocked());
    }

    /** OwnerPasswordGuard runs first: reaching this method means the password was right. */
    @PostMapping("/check")
    public Map<String, Boolean> check() {
        return Map.of("ok", true);
    }
}
