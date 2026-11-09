package com.secondstitch.auth;

public class TooManyAttemptsException extends RuntimeException {

    public TooManyAttemptsException() {
        super("Too many wrong passwords. Try again in 15 minutes.");
    }
}
