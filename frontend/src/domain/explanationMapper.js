/**
 * Maps the backend's structured explanation JSON (see
 * generation.model.ExplanationPayload) into the shape ExplanationRenderer
 * consumes - components never touch raw job resultPayload JSON directly.
 */
export function mapExplanation(json) {
  return {
    version: json.version,
    suggestedTitle: json.suggestedTitle,
    overview: json.overview,
    sections: (json.sections ?? []).map((s) => ({
      heading: s.heading,
      type: s.type, // "concept" | "analogy" | "example"
      body: s.body ?? "",
      bullets: s.bullets ?? [],
    })),
  };
}

/**
 * Flattens the structured explanation back into plain text, for when
 * the user picks "use the AI's explanation" and that text needs to
 * become the conceptText sent to the Visualize job - Visualize takes a
 * plain string, not structured JSON.
 */
export function explanationToPlainText(explanation) {
  const parts = [explanation.suggestedTitle, "", explanation.overview];
  for (const section of explanation.sections) {
    parts.push("", section.heading + ":");
    if (section.bullets.length > 0) {
      parts.push(...section.bullets.map((b) => `- ${b}`));
    } else {
      parts.push(section.body);
    }
  }
  return parts.join("\n");
}
