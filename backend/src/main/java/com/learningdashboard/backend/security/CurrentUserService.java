package com.learningdashboard.backend.security;

import com.learningdashboard.backend.user.User;
import org.springframework.stereotype.Service;

/**
 * The only sanctioned way application code asks "who is making this
 * request". Every endpoint that touches user-owned data (concepts, jobs,
 * artifacts) must call this and filter/compare against the returned id —
 * never trust a user id supplied in a request body or path alone.
 */
@Service
public class CurrentUserService {

    public User requireCurrentUser() {
        User user = CurrentUserContext.get();
        if (user == null) {
            // Shouldn't happen: SecurityConfig requires authentication on every
            // non-actuator route before this can be reached. Fail loudly if it
            // ever does, rather than silently proceeding as "no user".
            throw new IllegalStateException("No authenticated user in context.");
        }
        return user;
    }
}
