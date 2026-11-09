package com.secondstitch.email;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A one-time code sent by email, for confirming an email address or resetting a password.
 *
 * Only a SHA-256 hash of the code is stored. The real code only exists in the email,
 * so even someone who could read the database couldn't use these links.
 */
@Entity
@Table(name = "email_token")
public class EmailToken {

    public enum Purpose { VERIFY_EMAIL, RESET_PASSWORD }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Purpose purpose;

    @Column(nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    /** When the link was used. A used link never works again. */
    private Instant usedAt;

    protected EmailToken() {
        // for JPA
    }

    public EmailToken(Long userId, Purpose purpose, String tokenHash, Instant createdAt, Instant expiresAt) {
        this.userId = userId;
        this.purpose = purpose;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public boolean isUsable(Instant now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    public Long getUserId() { return userId; }

    public Purpose getPurpose() { return purpose; }

    public Instant getCreatedAt() { return createdAt; }
}
