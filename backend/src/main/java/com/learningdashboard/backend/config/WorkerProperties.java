package com.learningdashboard.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.worker")
public class WorkerProperties {
    private boolean pollEnabled = true;

    public boolean isPollEnabled() { return pollEnabled; }
    public void setPollEnabled(boolean pollEnabled) { this.pollEnabled = pollEnabled; }
}
