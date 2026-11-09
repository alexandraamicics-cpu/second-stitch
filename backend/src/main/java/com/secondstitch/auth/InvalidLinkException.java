package com.secondstitch.auth;

/** An email link that's wrong, expired or already used. */
public class InvalidLinkException extends RuntimeException {

    public InvalidLinkException(String message) {
        super(message);
    }
}
