package com.secondstitch.auth;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Who can call what.
 *
 *  - Signing up, confirming your email, logging in and resetting a password: anyone.
 *  - Photos: anyone with the photo's link (the link contains a long random code; see ItemService).
 *  - Everything else under /api: only with a valid login token
 *    ("Authorization: Bearer <token>"), and only for your own data.
 *
 * The token is a JWT: a small signed note saying "this is user 7, valid until Friday".
 * The server signs it with a secret key, so nobody can fake or edit one.
 */
@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private static final Pattern PHOTO_PATH = Pattern.compile("^/api/items/\\d+/photo$");

    /** Everything you need before you're logged in. */
    private static final Set<String> PUBLIC_AUTH_PATHS = Set.of(
            "/api/auth/signup",
            "/api/auth/verify",
            "/api/auth/resend-verification",
            "/api/auth/login",
            "/api/auth/forgot-password",
            "/api/auth/reset-password");

    @Bean
    SecurityFilterChain api(HttpSecurity http, JsonAuthEntryPoint entryPoint) throws Exception {
        RequestMatcher signUpOrLogIn = request -> "POST".equals(request.getMethod())
                && PUBLIC_AUTH_PATHS.contains(request.getRequestURI());
        RequestMatcher photo = request -> "GET".equals(request.getMethod())
                && PHOTO_PATH.matcher(request.getRequestURI()).matches();
        RequestMatcher api = request -> request.getRequestURI().startsWith("/api/");

        http
                // Uses the CORS rules from CorsConfig
                .cors(Customizer.withDefaults())
                // CSRF protects cookie-based logins. We send tokens in a header instead, so it isn't needed.
                .csrf(csrf -> csrf.disable())
                // No server-side sessions: every request carries its token
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(signUpOrLogIn, photo).permitAll()
                        .requestMatchers(api).authenticated()
                        .anyRequest().permitAll()) // e.g. the H2 console on your laptop
                // Read "Authorization: Bearer <token>" and check its signature and expiry
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(entryPoint))
                .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint))
                // Lets the H2 console (local only) show inside its frame
                .headers(h -> h.frameOptions(f -> f.sameOrigin()));
        return http.build();
    }

    /** BCrypt is slow on purpose, so guessing passwords from a stolen database takes ages. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * The secret key that signs tokens, from JWT_SECRET.
     * Without one, a random key is made at start-up: fine on your laptop, but then every
     * restart logs everyone out, so always set JWT_SECRET on the live site.
     */
    @Bean
    SecretKey jwtKey(@Value("${app.jwt-secret:}") String secret) {
        byte[] keyBytes;
        if (secret.isBlank()) {
            keyBytes = new byte[32];
            new SecureRandom().nextBytes(keyBytes);
            log.warn("No JWT_SECRET set: using a random key, so restarting logs everyone out.");
        } else {
            keyBytes = sha256(secret); // any length of secret becomes a 256-bit key
        }
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtKey) {
        return NimbusJwtDecoder.withSecretKey(jwtKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    private static byte[] sha256(String text) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
