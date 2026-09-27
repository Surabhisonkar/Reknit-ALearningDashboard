package com.learningdashboard.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code app.gdpr.*}. {@code purgeEnabled} defaults to
 * {@code false} deliberately: the purge job runs a hard, cascading
 * delete, and if every ECS task ran it on its own schedule that would
 * still be safe (deleting an already-deleted user is a no-op) but noisy
 * and redundant. Enable it on exactly one instance/service in
 * production (a small dedicated scheduled task is the cleanest way -
 * see the README).
 */
@ConfigurationProperties(prefix = "app.gdpr")
public class GdprProperties {

    private int gracePeriodDays = 30;
    private boolean purgeEnabled = false;

    public int getGracePeriodDays() { return gracePeriodDays; }
    public void setGracePeriodDays(int gracePeriodDays) { this.gracePeriodDays = gracePeriodDays; }
    public boolean isPurgeEnabled() { return purgeEnabled; }
    public void setPurgeEnabled(boolean purgeEnabled) { this.purgeEnabled = purgeEnabled; }
}
