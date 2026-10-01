package com.learningdashboard.backend.web;

import com.learningdashboard.backend.folder.FolderLabel;
import com.learningdashboard.backend.folder.FolderSummary;
import com.learningdashboard.backend.web.dto.FolderResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningdashboard.backend.concept.Concept;
import com.learningdashboard.backend.concept.ConceptNote;
import com.learningdashboard.backend.concept.ConceptVersion;
import com.learningdashboard.backend.rag.RelatedConcept;
import com.learningdashboard.backend.generation.job.GenerationJob;
import com.learningdashboard.backend.web.dto.ConceptNoteResponse;
import com.learningdashboard.backend.web.dto.ConceptResponse;
import com.learningdashboard.backend.web.dto.ConceptVersionResponse;
import com.learningdashboard.backend.web.dto.ConceptVersionSummaryResponse;
import com.learningdashboard.backend.web.dto.JobResponse;
import com.learningdashboard.backend.web.dto.RelatedConceptResponse;
import org.springframework.stereotype.Component;

@Component
public class ResponseMapper {

    private final ObjectMapper objectMapper;

    public ResponseMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JobResponse toJobResponse(GenerationJob job) {
        return new JobResponse(
                job.getId(),
                job.getJobType().name(),
                job.getStatus().name(),
                parseOrNull(job.getResultPayload()),
                job.getConceptId(),
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getUpdatedAt());
    }

    /** {@code folder} is null for an unfiled concept. */
    public ConceptResponse toConceptResponse(Concept concept, FolderLabel folder) {
        return new ConceptResponse(
                concept.getId(),
                concept.getTitle(),
                concept.getSummary(),
                folder != null ? folder.name() : "",
                folder != null ? folder.id() : null,
                folder != null ? folder.color().key() : null,
                concept.getVisualizationType(),
                parseOrNull(concept.getVisualizationPayload()),
                concept.getCurrentVersion(),
                concept.getCreatedAt(),
                concept.getUpdatedAt());
    }

    public ConceptVersionSummaryResponse toConceptVersionSummaryResponse(ConceptVersion version) {
        return new ConceptVersionSummaryResponse(version.getVersion(), version.getTitle(), version.getCreatedAt());
    }

    public ConceptVersionResponse toConceptVersionResponse(ConceptVersion version) {
        return new ConceptVersionResponse(
                version.getVersion(),
                version.getTitle(),
                version.getSummary(),
                version.getVisualizationType(),
                parseOrNull(version.getVisualizationPayload()),
                version.getCreatedAt());
    }

    public FolderResponse toFolderResponse(FolderSummary summary) {
        var folder = summary.folder();
        return new FolderResponse(folder.getId(), folder.getName(), folder.getColor(), summary.conceptCount(),
                folder.getCreatedAt());
    }

    public ConceptNoteResponse toConceptNoteResponse(ConceptNote note) {
        return new ConceptNoteResponse(note.getId(), note.getContent(), note.getSource(), note.getCreatedAt());
    }

    public RelatedConceptResponse toRelatedConceptResponse(RelatedConcept related) {
        return new RelatedConceptResponse(related.conceptId(), related.title(), related.summary(), related.distance());
    }

    private JsonNode parseOrNull(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null; // shouldn't happen - this string only ever comes from our own serialization
        }
    }
}
