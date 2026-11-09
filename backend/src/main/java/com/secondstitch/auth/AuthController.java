package com.secondstitch.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 *   POST /api/auth/signup               { email, password, name } → { message }  (sends "Confirm your email")
 *   POST /api/auth/verify               { token }                 → { token, user }  (logged in)
 *   POST /api/auth/resend-verification  { email }                 → { message }
 *   POST /api/auth/login                { email, password }       → { token, user }  (403 if not confirmed)
 *   POST /api/auth/forgot-password      { email }                 → { message }  (sends "Reset your password")
 *   POST /api/auth/reset-password       { token, password }       → { token, user }  (logged in)
 *   GET  /api/auth/me                   (needs a login token)     → { id, email, name }
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse signUp(@Valid @RequestBody SignupRequest request) {
        return service.signUp(request);
    }

    @PostMapping("/verify")
    public AuthResponse verify(@Valid @RequestBody VerifyRequest request) {
        return service.verifyEmail(request);
    }

    @PostMapping("/resend-verification")
    public MessageResponse resendVerification(@Valid @RequestBody EmailRequest request) {
        return service.resendVerification(request);
    }

    @PostMapping("/login")
    public AuthResponse logIn(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return service.logIn(request, ClientAddress.of(http));
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody EmailRequest request) {
        return service.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public AuthResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return service.resetPassword(request);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return service.me(CurrentUser.id(jwt));
    }
}
