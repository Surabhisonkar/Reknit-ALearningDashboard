package com.learningdashboard.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Runs once Spring Security has already validated the Cognito JWT
 * (signature, issuer, expiry). Its only job: make sure a local {@code
 * users} row exists for this token's {@code sub} so the rest of the app
 * has a stable local user id to own data by, without ever needing to call
 * back out to Cognito. First request for a new Cognito user creates the
 * row; every request after that is a cheap lookup.
 */
@Component
public class CognitoUserProvisioningFilter extends OncePerRequestFilter {

    private final UserProvisioningService userProvisioningService;

    public CognitoUserProvisioningFilter(UserProvisioningService userProvisioningService) {
        this.userProvisioningService = userProvisioningService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            var localUser = userProvisioningService.findOrCreateUser(jwt);
            CurrentUserContext.set(localUser);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            CurrentUserContext.clear();
        }
    }
}