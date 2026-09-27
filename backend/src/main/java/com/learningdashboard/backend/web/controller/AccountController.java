package com.learningdashboard.backend.web.controller;

import com.learningdashboard.backend.security.CurrentUserService;
import com.learningdashboard.backend.security.UserProvisioningService;
import com.learningdashboard.backend.user.DataDeletionService;
import com.learningdashboard.backend.web.dto.AccountResponse;
import com.learningdashboard.backend.web.dto.ProfileUpdateRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Self-service account management. {@code DELETE /api/account} starts
 * the GDPR deletion grace period rather than deleting immediately - see
 * {@link DataDeletionService} for why, and for what "delete" actually
 * cascades to.
 */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final CurrentUserService currentUserService;
    private final DataDeletionService dataDeletionService;
    private final UserProvisioningService userProvisioningService;

    public AccountController(CurrentUserService currentUserService, DataDeletionService dataDeletionService,
                              UserProvisioningService userProvisioningService) {
        this.currentUserService = currentUserService;
        this.dataDeletionService = dataDeletionService;
        this.userProvisioningService = userProvisioningService;
    }

    @GetMapping
    public AccountResponse get() {
        var user = currentUserService.requireCurrentUser();
        Instant effectiveAt = dataDeletionService.getDeletionEffectiveAt(user.getId());
        return new AccountResponse(user.getId(), user.getEmail(), user.getDisplayName(), effectiveAt != null, effectiveAt);
    }

    /**
     * Called once by the frontend right after login, with values decoded
     * from its own ID token - the access token this endpoint is actually
     * authenticated by doesn't reliably carry email/name (see {@link
     * UserProvisioningService#updateProfile}). Never affects
     * authentication/authorization, display metadata only.
     */
    @PutMapping("/profile")
    public AccountResponse updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        var user = currentUserService.requireCurrentUser();
        var updated = userProvisioningService.updateProfile(user.getId(), request.getEmail(), request.getDisplayName());
        Instant effectiveAt = dataDeletionService.getDeletionEffectiveAt(updated.getId());
        return new AccountResponse(updated.getId(), updated.getEmail(), updated.getDisplayName(), effectiveAt != null, effectiveAt);
    }

    @DeleteMapping
    public AccountResponse requestDeletion() {
        var user = currentUserService.requireCurrentUser();
        Instant effectiveAt = dataDeletionService.requestDeletion(user.getId());
        return new AccountResponse(user.getId(), user.getEmail(), user.getDisplayName(), true, effectiveAt);
    }

    @PostMapping("/cancel-deletion")
    public AccountResponse cancelDeletion() {
        var user = currentUserService.requireCurrentUser();
        dataDeletionService.cancelDeletion(user.getId());
        return new AccountResponse(user.getId(), user.getEmail(), user.getDisplayName(), false, null);
    }
}

