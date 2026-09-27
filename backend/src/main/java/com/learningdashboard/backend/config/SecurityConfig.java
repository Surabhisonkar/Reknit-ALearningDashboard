package com.learningdashboard.backend.config;

import com.learningdashboard.backend.security.CognitoUserProvisioningFilter;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Every non-actuator, non-OPTIONS request must present a valid Cognito
 * access token (JWT), validated against {@code app.cognito} / the standard
 * Spring {@code spring.security.oauth2.resourceserver.jwt.issuer-uri}
 * (JWKs fetched and cached automatically). There is no session state —
 * the service stays stateless, which is what lets it scale horizontally.
 *
 * <p>CORS is an explicit allow-list from config ({@code
 * CORS_ALLOWED_ORIGINS}), never {@code "*"}. HTTPS itself is enforced at
 * the ALB (HTTP listener redirects to HTTPS; see the Terraform in
 * infra/), not in application code — that's the correct layer for it.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CognitoUserProvisioningFilter provisioningFilter;
    private final CorsProperties corsProperties;

    public SecurityConfig(CognitoUserProvisioningFilter provisioningFilter, CorsProperties corsProperties) {
        this.provisioningFilter = provisioningFilter;
        this.corsProperties = corsProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable()) // stateless bearer-token API, no cookies to forge
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                    .anyRequest().authenticated())
            .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)
            .addFilterAfter(provisioningFilter, BasicAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Cognito puts custom/group claims under different names depending on
     * pool config; we only need "is this a valid token", authorization is
     * ownership-based (see CurrentUserService), not role-based, so no
     * scope-to-authority mapping is required here beyond the default.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        var authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("SCOPE_");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.copyOf(corsProperties.allowedOrigins()));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(false); // bearer tokens, not cookies - no credentials needed
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
