package com.learningdashboard.backend.user;

import com.learningdashboard.backend.config.GdprProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Runs daily; see {@link GdprProperties#isPurgeEnabled()} for why this is opt-in per deployment. */
@Component
public class AccountPurgeJob {

    private static final Logger log = LoggerFactory.getLogger(AccountPurgeJob.class);

    private final DataDeletionService dataDeletionService;
    private final GdprProperties gdprProperties;

    public AccountPurgeJob(DataDeletionService dataDeletionService, GdprProperties gdprProperties) {
        this.dataDeletionService = dataDeletionService;
        this.gdprProperties = gdprProperties;
    }

    @Scheduled(cron = "0 0 3 * * *") // 03:00 daily, server's default timezone (set TZ explicitly in your task definition)
    public void run() {
        if (!gdprProperties.isPurgeEnabled()) {
            return;
        }
        int purged = dataDeletionService.purgeUsersPastGracePeriod();
        if (purged > 0) {
            log.info("GDPR purge job removed {} user(s) past their grace period.", purged);
        }
    }
}
