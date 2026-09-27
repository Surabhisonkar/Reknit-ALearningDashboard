package com.learningdashboard.backend.common.exception;

/**
 * Deliberately distinct from {@link NotFoundException}: this is the
 * "well-formed request, wrong owner" case that the P0 security brief
 * calls out explicitly — every endpoint touching user data must check
 * ownership, not just that the request parses.
 *
 * <p>The HTTP mapping intentionally returns 404, not 403, for
 * cross-tenant access (see GlobalExceptionHandler) - this avoids
 * confirming to an attacker that a given job/concept id exists at all
 * (an enumeration-safe posture), while this exception type still keeps
 * the *reason* distinct in logs from a genuine not-found.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
