package com.secondstitch.auth;

/** Right password, but the email hasn't been confirmed yet. Sent as a 403 with code EMAIL_NOT_VERIFIED. */
public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException() {
        super("Confirm your email first. Check your inbox for the link, or send it again.");
    }
}
