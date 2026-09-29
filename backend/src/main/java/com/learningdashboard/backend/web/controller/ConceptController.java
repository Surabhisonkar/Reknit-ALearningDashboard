package com.learningdashboard.backend.web.controller;

import com.learningdashboard.backend.concept.ConceptSaveService;
import com.learningdashboard.backend.concept.ConceptService;
import com.learningdashboard.backend.concept.DuplicateTitlePolicy;
import com.learningdashboard.backend.concept.SaveConceptCommand;
import com.learningdashboard.backend.concept.SaveOutcome;
import com.learningdashboard.backend.rag.EmbeddingIndexService;
import com.learningdashboard.backend.security.CurrentUserService;
import com.learningdashboard.backend.web.ConceptResponseAssembler;
import com.learningdashboard.backend.web.ResponseMapper;
import com.learningdashboard.backend.web.dto.ConceptRenameRequest;
import com.learningdashboard.backend.web.dto.ConceptResponse;
import com.learningdashboard.backend.web.dto.ConceptSaveRequest;
import com.learningdashboard.backend.web.dto.ConceptVersionResponse;
import com.learningdashboard.backend.web.dto.ConceptVersionSummaryResponse;
import jakarta.validation.Valid;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/concepts")
public class ConceptController {

    private final ConceptService conceptService;
    private final ConceptSaveService conceptSaveService;
    private final EmbeddingIndexService embeddingIndexService;
    private final CurrentUserService currentUserService;
    private final ResponseMapper responseMapper;
    private final ConceptResponseAssembler assembler;

    public ConceptController(ConceptService conceptService, ConceptSaveService conceptSaveService,
                              EmbeddingIndexService embeddingIndexService,
                              CurrentUserService currentUserService, ResponseMapper responseMapper,
                              ConceptResponseAssembler assembler) {
        this.conceptService = conceptService;
        this.conceptSaveService = conceptSaveService;
        this.embeddingIndexService = embeddingIndexService;
        this.currentUserService = currentUserService;
        this.responseMapper = responseMapper;
        this.assembler = assembler;
    }

    @GetMapping
    public List<ConceptResponse> list(@RequestParam(required = false) String folder) {
        var user = currentUserService.requireCurrentUser();
        return assembler.toResponses(user.getId(), conceptService.listForUser(user.getId(), folder));
    }

    /**
     * Confirm-save: promotes a completed Visualize job's draft into a real
     * concept (+ its version-1 row), attaching the draft's generated assets
     * and queueing its embedding. The duplicate-title check runs here,
     * before anything is written - a collision under the default
     * {@code onDuplicate=REJECT} returns 409 (see GlobalExceptionHandler)
     * and the client re-sends with a new {@code title}, {@code REPLACE} or
     * {@code KEEP_BOTH}. Returns 201 on create, 200 if this draft was
     * already saved (idempotent).
     */
    @PostMapping
    public ResponseEntity<ConceptResponse> save(@Valid @RequestBody ConceptSaveRequest request) {
        var user = currentUserService.requireCurrentUser();
        SaveOutcome outcome = conceptSaveService.save(new SaveConceptCommand(
                request.getJobId(), user.getId(), request.getTitle(), request.getFolder(),
                DuplicateTitlePolicy.valueOf(request.getOnDuplicate())));
        return ResponseEntity.status(outcome.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(assembler.toResponse(outcome.concept()));
    }

    /**
     * Spark's feed. {@code excludeIds} is a comma-separated list of concept
     * ids the client has already shown this session, so "load more" pages
     * through the pool instead of repeating cards - see {@link
     * com.learningdashboard.backend.concept.ConceptService#randomSparkFeed}.
     * Spring resolves this literal path ahead of {@code /{id}} regardless
     * of declaration order, so it's safe alongside the get-by-id route below.
     */
    @GetMapping("/spark-feed")
    public List<ConceptResponse> sparkFeed(
            @RequestParam(required = false) String folder,
            @RequestParam(required = false, defaultValue = "") String excludeIds,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        var user = currentUserService.requireCurrentUser();
        int safeLimit = Math.min(Math.max(limit, 1), 25); // guard against an accidentally huge request
        return assembler.toResponses(user.getId(),
                conceptService.randomSparkFeed(user.getId(), folder, parseUuidCsv(excludeIds), safeLimit));
    }

    @GetMapping("/{id}")
    public ConceptResponse get(@PathVariable UUID id) {
        var user = currentUserService.requireCurrentUser();
        return assembler.toResponse(conceptService.requireOwnedConcept(id, user.getId()));
    }

    /**
     * Version metadata (number, title, created_at) for the Workspace
     * version switcher - no payload, see the {id}/versions/{version}
     * route below for one version's full content. Spring resolves this
     * literal path ahead of {@code /{id}}, same as {@code /spark-feed}
     * above (and {@code /folders}, now on ConceptFolderController).
     */
    @GetMapping("/{id}/versions")
    public List<ConceptVersionSummaryResponse> versions(@PathVariable UUID id) {
        var user = currentUserService.requireCurrentUser();
        conceptService.requireOwnedConcept(id, user.getId()); // ownership check - a version has no owner of its own
        return conceptService.listVersions(id).stream()
                .map(responseMapper::toConceptVersionSummaryResponse)
                .toList();
    }

    /** Full content of one historical version - the concept's own cached fields only ever reflect the current version. */
    @GetMapping("/{id}/versions/{version}")
    public ConceptVersionResponse version(@PathVariable UUID id, @PathVariable int version) {
        var user = currentUserService.requireCurrentUser();
        conceptService.requireOwnedConcept(id, user.getId());
        return responseMapper.toConceptVersionResponse(conceptService.requireVersion(id, version));
    }

    /** Renames a saved concept. (Duplicate-name resolution now happens at confirm-save time, via {@code POST /api/concepts}.) */
    @PatchMapping("/{id}")
    public ConceptResponse rename(@PathVariable UUID id, @Valid @RequestBody ConceptRenameRequest request) {
        var user = currentUserService.requireCurrentUser();
        return assembler.toResponse(conceptService.renameOwnedConcept(id, user.getId(), request.getTitle()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        var user = currentUserService.requireCurrentUser();
        conceptService.deleteOwnedConcept(id, user.getId());
        embeddingIndexService.removeIndex(id); // keep RAG index consistent with what the user can still see
        return ResponseEntity.noContent().build();
    }

    private Set<UUID> parseUuidCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        Set<UUID> ids = new LinkedHashSet<>();
        for (String raw : csv.split(",")) {
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                ids.add(UUID.fromString(trimmed));
            } catch (IllegalArgumentException ignored) {
                // Malformed id from the client - skip it rather than 400ing the whole feed request.
            }
        }
        return ids;
    }
}
