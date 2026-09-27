package com.learningdashboard.backend.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code app.content-safety.disallowed-terms} - a comma-separated
 * list an operator can extend without a code change once shared/visible
 * content features exist. Empty by default: this class's real job today
 * is the structural checks (control characters, markup injection) that
 * don't depend on any list at all - see {@code ContentSafetyValidator}.
 */
@ConfigurationProperties(prefix = "app.content-safety")
public class ContentSafetyProperties {

    private List<String> disallowedTerms = List.of();

    public List<String> getDisallowedTerms() { return disallowedTerms; }
    public void setDisallowedTerms(List<String> disallowedTerms) { this.disallowedTerms = disallowedTerms; }
}
