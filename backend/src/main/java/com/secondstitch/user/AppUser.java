package com.secondstitch.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Someone with an account. The table is called "app_user" because "user"
 * is a reserved word in PostgreSQL.
 *
 * The password is never stored, only its BCrypt hash: a one-way scramble.
 * At log-in, BCrypt scrambles what you typed the same way and compares.
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Always stored in lowercase, so "Ana@Mail.com" and "ana@mail.com" are the same account. */
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private String name;

    /** True once they've clicked the link in the "Confirm your email" message. */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean emailVerified;

    private Instant createdAt = Instant.now();

    protected AppUser() {
        // for JPA
    }

    public AppUser(String email, String passwordHash, String name) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
    }

    public Long getId() { return id; }

    public String getEmail() { return email; }

    public String getPasswordHash() { return passwordHash; }

    public String getName() { return name; }

    public boolean isEmailVerified() { return emailVerified; }

    public void markEmailVerified() { this.emailVerified = true; }

    public void changePasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Instant getCreatedAt() { return createdAt; }
}
