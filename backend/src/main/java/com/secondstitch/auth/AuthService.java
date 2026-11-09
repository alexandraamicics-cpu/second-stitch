package com.secondstitch.auth;

import com.secondstitch.common.ConflictException;
import com.secondstitch.common.NotFoundException;
import com.secondstitch.email.AuthEmails;
import com.secondstitch.email.EmailToken;
import com.secondstitch.email.EmailTokens;
import com.secondstitch.item.ItemRepository;
import com.secondstitch.outfit.OutfitRepository;
import com.secondstitch.user.AppUser;
import com.secondstitch.user.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Accounts: sign up, confirm email, log in, forgot password.
 *
 *   sign up        → account saved (not confirmed) → "Confirm your email" link sent
 *   click the link → confirmed → logged in (and the closet is set up)
 *   log in         → only works once confirmed
 *   forgot         → "Reset your password" link sent (if the account exists)
 *   reset link     → new password saved → logged in
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /** Same reply whether or not the account exists, so nobody can check which emails have accounts. */
    static final String RESEND_REPLY =
            "If that account still needs confirming, we've sent a new link. Check your inbox (and spam folder).";
    static final String FORGOT_REPLY =
            "If there's an account with that email, we've sent a link to reset the password. "
                    + "Check your inbox (and spam folder).";

    private final AppUserRepository users;
    private final ItemRepository items;
    private final OutfitRepository outfits;
    private final PasswordEncoder passwords;
    private final TokenService tokens;
    private final FailedAttempts failedAttempts;
    private final SampleCloset sampleCloset;
    private final EmailTokens emailTokens;
    private final AuthEmails emails;
    private final boolean giveSampleCloset;

    /** Checked against when the email doesn't exist, so a wrong email takes as long as a wrong password. */
    private final String dummyHash;

    public AuthService(AppUserRepository users, ItemRepository items, OutfitRepository outfits,
                       PasswordEncoder passwords, TokenService tokens, FailedAttempts failedAttempts,
                       SampleCloset sampleCloset, EmailTokens emailTokens, AuthEmails emails,
                       @Value("${app.seed-sample-data:false}") boolean giveSampleCloset) {
        this.users = users;
        this.items = items;
        this.outfits = outfits;
        this.passwords = passwords;
        this.tokens = tokens;
        this.failedAttempts = failedAttempts;
        this.sampleCloset = sampleCloset;
        this.emailTokens = emailTokens;
        this.emails = emails;
        this.giveSampleCloset = giveSampleCloset;
        this.dummyHash = passwords.encode("not-a-real-password");
    }

    @Transactional
    public MessageResponse signUp(SignupRequest request) {
        String email = normalise(request.email());
        if (users.existsByEmail(email)) {
            throw new ConflictException(
                    "There's already an account with that email. Log in, or use Forgot password.");
        }
        String name = request.name() == null || request.name().isBlank() ? null : request.name().trim();
        AppUser user = users.save(new AppUser(email, passwords.encode(request.password()), name));

        sendVerification(user);
        return new MessageResponse("We've sent a link to " + email + ". Click it to confirm your email "
                + "and open your closet. Check your spam folder if it isn't there in a minute.");
    }

    @Transactional
    public AuthResponse verifyEmail(VerifyRequest request) {
        Long userId = emailTokens.use(request.token(), EmailToken.Purpose.VERIFY_EMAIL)
                .orElseThrow(() -> new InvalidLinkException(
                        "That link has expired or was already used. Log in to get a new one."));
        AppUser user = users.findById(userId).orElseThrow(() -> new NotFoundException("User", userId));
        confirm(user);
        return new AuthResponse(tokens.issue(user), UserResponse.from(user));
    }

    @Transactional
    public MessageResponse resendVerification(EmailRequest request) {
        users.findByEmail(normalise(request.email()))
                .filter(user -> !user.isEmailVerified())
                .filter(user -> !emailTokens.sentRecently(user.getId(), EmailToken.Purpose.VERIFY_EMAIL))
                .ifPresent(this::sendVerification);
        return new MessageResponse(RESEND_REPLY);
    }

    @Transactional
    public AuthResponse logIn(LoginRequest request, String clientAddress) {
        if (failedAttempts.tooMany(clientAddress)) {
            throw new TooManyAttemptsException();
        }
        AppUser user = users.findByEmail(normalise(request.email())).orElse(null);
        String hash = user == null ? dummyHash : user.getPasswordHash();
        boolean passwordMatches = passwords.matches(request.password(), hash);

        if (user == null || !passwordMatches) {
            failedAttempts.record(clientAddress);
            throw new LoginFailedException();
        }
        failedAttempts.clear(clientAddress);
        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException();
        }
        return new AuthResponse(tokens.issue(user), UserResponse.from(user));
    }

    @Transactional
    public MessageResponse forgotPassword(EmailRequest request) {
        users.findByEmail(normalise(request.email()))
                .filter(user -> !emailTokens.sentRecently(user.getId(), EmailToken.Purpose.RESET_PASSWORD))
                .ifPresent(user -> {
                    String code = emailTokens.create(user.getId(), EmailToken.Purpose.RESET_PASSWORD);
                    emails.sendPasswordReset(user.getEmail(), user.getName(), code);
                });
        return new MessageResponse(FORGOT_REPLY);
    }

    @Transactional
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        Long userId = emailTokens.use(request.token(), EmailToken.Purpose.RESET_PASSWORD)
                .orElseThrow(() -> new InvalidLinkException(
                        "That reset link has expired or was already used. Ask for a new one."));
        AppUser user = users.findById(userId).orElseThrow(() -> new NotFoundException("User", userId));
        user.changePasswordHash(passwords.encode(request.password()));
        // Getting the reset email proves they own the address too
        confirm(user);
        return new AuthResponse(tokens.issue(user), UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return users.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("User", userId));
    }

    private void sendVerification(AppUser user) {
        String code = emailTokens.create(user.getId(), EmailToken.Purpose.VERIFY_EMAIL);
        emails.sendVerification(user.getEmail(), user.getName(), code);
    }

    /** Marks the email as confirmed. The first time, this also sets up their closet. */
    private void confirm(AppUser user) {
        if (user.isEmailVerified()) {
            return;
        }
        user.markEmailVerified();
        users.saveAndFlush(user);

        // The very first confirmed account takes over any closet made before accounts existed
        boolean firstAccount = users.countByEmailVerifiedTrue() == 1;
        if (firstAccount && items.existsByOwnerIdIsNull()) {
            int claimedItems = items.claimUnowned(user.getId());
            int claimedOutfits = outfits.claimUnowned(user.getId());
            log.info("First account claimed {} existing pieces and {} outfits", claimedItems, claimedOutfits);
        } else if (giveSampleCloset) {
            sampleCloset.createFor(user.getId());
        }
    }

    private static String normalise(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
