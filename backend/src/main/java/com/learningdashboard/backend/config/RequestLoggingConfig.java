package com.learningdashboard.backend.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

/**
 * Every inbound request logged (method + path + status, via a before/after
 * pair), registered at the highest filter precedence so it runs even for
 * requests Spring Security rejects (401/403) or CORS blocks before they
 * ever reach a controller - previously those left zero trace anywhere in
 * the console, which is exactly what made a real "Failed to fetch" report
 * from the frontend impossible to diagnose from the backend side alone.
 *
 * <p>Never logs the request body or headers ({@code includePayload}/
 * {@code includeHeaders} both off) - no bearer tokens, no request content,
 * consistent with the "no request bodies/tokens in logs" rule already
 * noted next to the console log pattern in application.yml.
 */
@Configuration
public class RequestLoggingConfig {

    @Bean
    public FilterRegistrationBean<CommonsRequestLoggingFilter> requestLoggingFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(false);
        filter.setIncludeHeaders(false);
        filter.setBeforeMessagePrefix("Incoming request [");
        filter.setBeforeMessageSuffix("]");
        filter.setAfterMessagePrefix("Completed request [");
        filter.setAfterMessageSuffix("]");

        FilterRegistrationBean<CommonsRequestLoggingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
