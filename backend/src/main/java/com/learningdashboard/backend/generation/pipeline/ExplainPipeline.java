package com.learningdashboard.backend.generation.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningdashboard.backend.common.exception.GenerationException;
import com.learningdashboard.backend.generation.model.ExplanationPayload;
import com.learningdashboard.backend.generation.prompt.ExplainPromptBuilder;
import com.learningdashboard.backend.generation.provider.TextGenerationProvider;
import com.learningdashboard.backend.generation.validation.ContentSafetyValidator;
import com.learningdashboard.backend.generation.validation.ExplanationValidator;
import org.springframework.stereotype.Component;

/**
 * Backs the "Explain" flow. Exactly one provider call — structuring a
 * visualization is a separate job type/pipeline entirely (see {@link
 * VisualizePipeline}), never bundled into this same call. {@code
 * userNotes} is always optional: this pipeline explains the topic on
 * its own knowledge when none are given.
 */
@Component
public class ExplainPipeline {

    private final TextGenerationProvider textProvider;
    private final ExplanationValidator explanationValidator;
    private final ContentSafetyValidator contentSafetyValidator;
    private final ObjectMapper objectMapper;

    public ExplainPipeline(TextGenerationProvider textProvider, ExplanationValidator explanationValidator,
                            ContentSafetyValidator contentSafetyValidator, ObjectMapper objectMapper) {
        this.textProvider = textProvider;
        this.explanationValidator = explanationValidator;
        this.contentSafetyValidator = contentSafetyValidator;
        this.objectMapper = objectMapper;
    }

    public Result run(String topic, String userNotes) {
        ExplainPromptBuilder promptBuilder = new ExplainPromptBuilder()
                .withTopic(topic)
                .withUserNotes(userNotes);

        String rawResponse = textProvider.generateText(promptBuilder.buildSystemPrompt(), promptBuilder.buildUserPrompt());
        JsonNode parsed = parseJsonSafely(rawResponse);

        explanationValidator.validate(parsed);
        ExplanationPayload explanation = objectMapper.convertValue(parsed, ExplanationPayload.class);

        contentSafetyValidator.validate("suggestedTitle", explanation.suggestedTitle());
        contentSafetyValidator.validate("overview", explanation.overview());
        for (ExplanationPayload.Section section : explanation.sections()) {
            contentSafetyValidator.validate("section.heading", section.heading());
            contentSafetyValidator.validate("section.body", section.body());
            if (section.bullets() != null) {
                section.bullets().forEach(bullet -> contentSafetyValidator.validate("section.bullet", bullet));
            }
        }

        return new Result(explanation, rawResponse);
    }

    private JsonNode parseJsonSafely(String rawText) {
        try {
            String cleaned = rawText.trim().replaceFirst("^```json\\n?", "").replaceFirst("```$", "");
            return objectMapper.readTree(cleaned);
        } catch (Exception e) {
            throw new GenerationException("The AI response wasn't valid JSON.", GenerationException.Code.INVALID_JSON);
        }
    }

    public record Result(ExplanationPayload explanation, String rawModelResponse) { }
}
