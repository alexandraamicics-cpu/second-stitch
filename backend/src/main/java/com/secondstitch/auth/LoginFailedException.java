package com.secondstitch.auth;

/** Wrong email or password. The message never says which, so it can't be used to find accounts. */
public class LoginFailedException extends RuntimeException {

    public LoginFailedException() {
        super("That email and password don't match");
    }
}
