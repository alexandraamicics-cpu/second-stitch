package com.secondstitch.common;

/** Thrown when an item or outfit id does not exist. Turned into a 404 by ApiExceptionHandler. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String what, Long id) {
        super(what + " " + id + " was not found");
    }
}
