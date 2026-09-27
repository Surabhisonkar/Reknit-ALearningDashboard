package com.learningdashboard.backend.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds {@code app.cors.allowed-origins} - a comma-separated allow-list, never "*". */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
