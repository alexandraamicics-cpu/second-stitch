package com.secondstitch.auth;

import com.secondstitch.user.AppUser;

/** The safe-to-share parts of an account. Never includes the password hash. */
public record UserResponse(Long id, String email, String name) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName());
    }
}
