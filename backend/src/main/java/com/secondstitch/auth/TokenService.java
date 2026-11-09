package com.secondstitch.auth;

import com.secondstitch.user.AppUser;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/** Creates the signed login token (JWT) handed out after signing up or logging in. */
@Service
public class TokenService {

    /** How long you stay logged in before you have to log in again. */
    static final Duration LIFETIME = Duration.ofDays(7);

    private final JwtEncoder encoder;

    public TokenService(JwtEncoder encoder) {
        this.encoder = encoder;
    }

    public String issue(AppUser user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("second-stitch")
                .issuedAt(now)
                .expiresAt(now.plus(LIFETIME))
                .subject(user.getId().toString()) // who this token belongs to
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
