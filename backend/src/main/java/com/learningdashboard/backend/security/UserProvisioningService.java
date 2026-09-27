package com.learningdashboard.backend.security;

import com.learningdashboard.backend.user.User;
import com.learningdashboard.backend.user.UserRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Finds or creates the local {@code users} row for an authenticated
 * Cognito principal. Kept separate from {@link CognitoUserProvisioningFilter}
 * so the {@code @Transactional} boundary only wraps the DB lookup/insert,
 * not the whole downstream filter chain.
 */
@Service
public class UserProvisioningService {

    private final UserRepository userRepository;

    public UserProvisioningService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User findOrCreateUser(Jwt jwt) {
        return userRepository.findByCognitoSub(jwt.getSubject())
                .orElseGet(() -> provisionNewUser(jwt));
    }

    /**
     * Cognito access tokens (what {@link #findOrCreateUser} validates and
     * reads) don't reliably carry {@code email}/{@code name} claims by
     * default - those live on the ID token instead, which a resource
     * server correctly never accepts as a bearer credential. So profile
     * display info is synced separately, from values the frontend already
     * has after decoding its own ID token client-side - see
     * AccountController's profile endpoint, called once right after login.
     * This never affects authentication/authorization, only display data.
     */
    @Transactional
    public User updateProfile(java.util.UUID userId, String email, String displayName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
        if (email != null && !email.isBlank()) {
            user.setEmail(email);
        }
        if (displayName != null && !displayName.isBlank()) {
            user.setDisplayName(displayName);
        }
        return userRepository.save(user);
    }

    private User provisionNewUser(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        String displayName = jwt.getClaimAsString("name");
        User user = new User(jwt.getSubject(), email != null ? email : "unknown", displayName);
        return userRepository.save(user);
    }
}