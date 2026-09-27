package com.learningdashboard.backend.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Deliberately not trusted for authentication/authorization - the caller
 * is already authenticated via a validated access token by the time this
 * is processed. These fields are display metadata only, sourced from the
 * ID token the frontend already decoded client-side.
 */
public class ProfileUpdateRequest {

    @Email(message = "email must be a valid email address")
    @Size(max = 320, message = "email must be at most 320 characters")
    private String email;

    @Size(max = 255, message = "displayName must be at most 255 characters")
    private String displayName;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}
