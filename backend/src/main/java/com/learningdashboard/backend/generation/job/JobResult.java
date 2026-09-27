package com.learningdashboard.backend.generation.job;

import java.util.UUID;

/**
 * What a {@link JobHandler} hands back for {@link GenerationJob#markCompleted}.
 *
 * @param rawModelResponse  exactly what the provider returned (audit), or null if no model was called
 * @param resultPayloadJson normalized result JSON the client polls for
 * @param conceptId         concept this job is linked to, or null (e.g. an unsaved draft)
 */
public record JobResult(String rawModelResponse, String resultPayloadJson, UUID conceptId) { }
