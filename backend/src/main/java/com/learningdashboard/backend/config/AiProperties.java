package com.learningdashboard.backend.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code app.ai.*}. The {@code *-provider-order} fields are the
 * fallback chains: first entry is tried first, later entries are only
 * used if everything before them fails (after its own retries and with
 * its circuit breaker closed). Adding a provider is a config change plus
 * one new bean in {@link com.learningdashboard.backend.generation.provider.ProviderFactory}
 * — nothing else in the codebase changes.
 */
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    private String anthropicApiKey;
    private String geminiApiKey;
    private String openaiApiKey;

    private List<String> textProviderOrder = List.of("claude", "gemini");
    private List<String> visualProviderOrder = List.of("gemini");
    private List<String> embeddingProviderOrder = List.of("gemini");

    private String claudeModel = "claude-sonnet-4-6";
    // gemini-2.5-flash was retired for this account (real 404 from a
    // worker-log run on 2026-09-20/21) - application.yml's
    // GEMINI_TEXT_MODEL default is the one that actually applies at
    // runtime; this field default is kept in sync for anything that
    // constructs AiProperties without loading application.yml (e.g. a
    // future unit test).
    private String geminiTextModel = "gemini-3.6-flash";
    private String geminiVisualModel = "gemini-2.5-flash-image";
    private String geminiEmbeddingModel = "gemini-embedding-001"; // text-embedding-004 was shut down 2026-01-14
    private String openaiTextModel = "gpt-4.1";

    private long requestTimeoutMs = 20_000L;
    private int maxRetries = 2;

    public String getAnthropicApiKey() { return anthropicApiKey; }
    public void setAnthropicApiKey(String v) { this.anthropicApiKey = v; }
    public String getGeminiApiKey() { return geminiApiKey; }
    public void setGeminiApiKey(String v) { this.geminiApiKey = v; }
    public String getOpenaiApiKey() { return openaiApiKey; }
    public void setOpenaiApiKey(String v) { this.openaiApiKey = v; }

    public List<String> getTextProviderOrder() { return textProviderOrder; }
    public void setTextProviderOrder(List<String> v) { this.textProviderOrder = v; }
    public List<String> getVisualProviderOrder() { return visualProviderOrder; }
    public void setVisualProviderOrder(List<String> v) { this.visualProviderOrder = v; }
    public List<String> getEmbeddingProviderOrder() { return embeddingProviderOrder; }
    public void setEmbeddingProviderOrder(List<String> v) { this.embeddingProviderOrder = v; }

    public String getClaudeModel() { return claudeModel; }
    public void setClaudeModel(String v) { this.claudeModel = v; }
    public String getGeminiTextModel() { return geminiTextModel; }
    public void setGeminiTextModel(String v) { this.geminiTextModel = v; }
    public String getGeminiVisualModel() { return geminiVisualModel; }
    public void setGeminiVisualModel(String v) { this.geminiVisualModel = v; }
    public String getGeminiEmbeddingModel() { return geminiEmbeddingModel; }
    public void setGeminiEmbeddingModel(String v) { this.geminiEmbeddingModel = v; }
    public String getOpenaiTextModel() { return openaiTextModel; }
    public void setOpenaiTextModel(String v) { this.openaiTextModel = v; }

    public long getRequestTimeoutMs() { return requestTimeoutMs; }
    public void setRequestTimeoutMs(long v) { this.requestTimeoutMs = v; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int v) { this.maxRetries = v; }
}
