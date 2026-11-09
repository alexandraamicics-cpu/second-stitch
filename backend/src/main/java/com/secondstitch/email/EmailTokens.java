package com.secondstitch.email;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/** Creates and checks the one-time codes that go into email links. */
@Service
public class EmailTokens {

    public static final Duration VERIFY_LIFETIME = Duration.ofHours(24);
    public static final Duration RESET_LIFETIME = Duration.ofHours(1);

    /** Wait at least this long before sending another email of the same kind to the same person. */
    public static final Duration COOLDOWN = Duration.ofMinutes(1);

    private final EmailTokenRepository tokens;
    private final SecureRandom random = new SecureRandom();

    public EmailTokens(EmailTokenRepository tokens) {
        this.tokens = tokens;
    }

    /** Makes a new code and returns it. Only its hash is saved. */
    public String create(Long userId, EmailToken.Purpose purpose) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        Instant now = Instant.now();
        Duration lifetime = purpose == EmailToken.Purpose.VERIFY_EMAIL ? VERIFY_LIFETIME : RESET_LIFETIME;
        tokens.save(new EmailToken(userId, purpose, hash(code), now, now.plus(lifetime)));
        return code;
    }

    /** The user this code belongs to, if the code is real, unused and not expired. Uses it up. */
    public Optional<Long> use(String code, EmailToken.Purpose purpose) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        Instant now = Instant.now();
        return tokens.findByTokenHashAndPurpose(hash(code), purpose)
                .filter(token -> token.isUsable(now))
                .map(token -> {
                    tokens.useAll(token.getUserId(), purpose, now);
                    return token.getUserId();
                });
    }

    /** True if an email of this kind went to this user less than a minute ago. */
    public boolean sentRecently(Long userId, EmailToken.Purpose purpose) {
        return tokens.findTopByUserIdAndPurposeOrderByCreatedAtDesc(userId, purpose)
                .map(token -> token.getCreatedAt().isAfter(Instant.now().minus(COOLDOWN)))
                .orElse(false);
    }

    public static String hash(String code) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
