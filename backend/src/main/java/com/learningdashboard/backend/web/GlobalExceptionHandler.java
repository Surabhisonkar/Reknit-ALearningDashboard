package com.learningdashboard.backend.web;

import com.learningdashboard.backend.concept.DraftNotSaveableException;
import com.learningdashboard.backend.concept.DuplicateTitleException;
import com.learningdashboard.backend.common.exception.ForbiddenException;
import com.learningdashboard.backend.common.exception.GenerationException;
import com.learningdashboard.backend.common.exception.NotFoundException;
import com.learningdashboard.backend.folder.DuplicateFolderNameException;
import com.learningdashboard.backend.folder.InvalidFolderRequestException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Central error -&gt; HTTP mapping. Hard rule enforced here: the client
 * never sees a stack trace, an exception class name, or provider
 * internals - only a small set of stable, generic messages. Anything
 * with real diagnostic value (stack trace, provider error body) goes to
 * the server log only, and {@code LoggingSanitizer} (see logging config)
 * is responsible for making sure request bodies/tokens never end up in
 * those logs either.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Confirm-save title collision under onDuplicate=REJECT -> 409, with
     * what the client needs to offer rename / replace / keep both.
     * Nothing was written.
     */
    @ExceptionHandler(DuplicateTitleException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateTitle(DuplicateTitleException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "A concept with this name already exists.");
        body.put("code", "DUPLICATE_TITLE");
        body.put("duplicateConceptId", ex.getDuplicateConceptId());
        body.put("title", ex.getTitle());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /** Confirm-save on a job that isn't a saveable draft -> 409 (not ready) or 410 (expired). Messages are ours, never provider text. */
    @ExceptionHandler(DuplicateFolderNameException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateFolderName(DuplicateFolderNameException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "A folder with this name already exists.");
        body.put("code", "DUPLICATE_FOLDER_NAME");
        body.put("name", ex.getName());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(InvalidFolderRequestException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidFolderRequest(InvalidFolderRequestException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(DraftNotSaveableException.class)
    public ResponseEntity<Map<String, Object>> handleDraftNotSaveable(DraftNotSaveableException ex) {
        boolean expired = ex.getReason() == DraftNotSaveableException.Reason.EXPIRED;
        return ResponseEntity.status(expired ? HttpStatus.GONE : HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage(), "code", expired ? "DRAFT_EXPIRED" : "DRAFT_NOT_READY"));
    }

    /** Genuinely missing resource -> 404. */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Not found."));
    }

    /**
     * Cross-tenant access attempt -> mapped to the same 404 as a genuine
     * not-found (enumeration-safe: doesn't confirm to the caller that the
     * id exists at all), but logged distinctly so we can actually see
     * these attempts in CloudWatch.
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenException ex) {
        log.warn("Ownership check failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Not found."));
    }

    /** Spring Security's own access-denied (e.g. a bad/expired JWT past initial validation) -> 403. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied."));
    }

    /** Request body failed Bean Validation -> 400. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Invalid request.");
        Map<String, String> details = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe -> details.put(fe.getField(), fe.getDefaultMessage()));
        body.put("details", details);
        return ResponseEntity.badRequest().body(body);
    }

    /** Malformed JSON body -> 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid request."));
    }

    /** Wrong HTTP verb on a mapped route -> 405. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(Map.of("error", "Method not allowed."));
    }

    /**
     * AI generation/validation failures. In the synchronous controllers
     * (there are none touching providers directly anymore - see below)
     * this would be the mapping; it's included for completeness and for
     * any future synchronous admin/debug endpoint, since the actual
     * generate/visualize flows never throw this back to an HTTP caller -
     * {@code JobProcessingService} catches it and records it on the job
     * row instead (see GenerationJob.errorMessage), which is what the
     * client actually polls and reads.
     */
    @ExceptionHandler(GenerationException.class)
    public ResponseEntity<Map<String, Object>> handleGeneration(GenerationException ex) {
        log.error("Generation failed [{}]: {}", ex.getCode(), ex.getDetails() != null ? ex.getDetails() : ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "The AI couldn't generate a usable result. Please try rephrasing."));
    }

    /** Anything unexpected -> 500, full detail to the log only. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("Unexpected error:", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Something went wrong. Please try again."));
    }
}
