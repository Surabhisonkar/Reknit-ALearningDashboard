package com.learningdashboard.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * One jar, two ECS services:
 *
 * <ul>
 *   <li><b>api</b> (default profile) — Spring MVC controllers behind the ALB,
 *       creates {@code GenerationJob} rows and enqueues them to SQS, never
 *       calls an LLM directly.</li>
 *   <li><b>worker</b> ({@code SPRING_PROFILES_ACTIVE=worker}) — no ALB
 *       target group, no public traffic. {@link
 *       com.learningdashboard.backend.generation.job.GenerationJobWorker}
 *       long-polls SQS and is the only place LLM providers are actually
 *       invoked.</li>
 * </ul>
 *
 * Splitting this way means a slow/stuck LLM call can never tie up an HTTP
 * request thread on the public-facing service, and the two services scale
 * independently in ECS (more worker tasks under generation load, without
 * touching the api tier).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
@EnableScheduling
public class BackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
