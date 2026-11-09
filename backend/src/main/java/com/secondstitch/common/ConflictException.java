package com.secondstitch.common;

/** The request clashes with something that already exists, e.g. an email that's already registered. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
