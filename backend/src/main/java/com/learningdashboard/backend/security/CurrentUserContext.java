package com.learningdashboard.backend.security;

import com.learningdashboard.backend.user.User;

/**
 * Thread-local holder for the current request's locally-provisioned
 * {@link User}, set by {@link CognitoUserProvisioningFilter} after token
 * validation and cleared at the end of every request. Controllers and
 * services read it via {@link CurrentUserService} rather than touching
 * this directly.
 */
final class CurrentUserContext {

    private static final ThreadLocal<User> CURRENT_USER = new ThreadLocal<>();

    private CurrentUserContext() { }

    static void set(User user) {
        CURRENT_USER.set(user);
    }

    static User get() {
        return CURRENT_USER.get();
    }

    static void clear() {
        CURRENT_USER.remove();
    }
}
