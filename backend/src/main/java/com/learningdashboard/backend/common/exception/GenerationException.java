package com.learningdashboard.backend.common.exception;

public class GenerationException extends RuntimeException {

    public enum Code {
        INVALID_JSON,
        SCHEMA_VALIDATION_FAILED,
        ALL_PROVIDERS_FAILED,
        CONTENT_SAFETY_REJECTED
    }

    private final Code code;
    private final transient Object details;

    public GenerationException(String message, Code code) {
        this(message, code, null);
    }

    public GenerationException(String message, Code code, Object details) {
        super(message);
        this.code = code;
        this.details = details;
    }

    public GenerationException(String message, Code code, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.details = null;
    }

    public Code getCode() { return code; }
    public Object getDetails() { return details; }
}
