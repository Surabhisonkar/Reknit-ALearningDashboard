const SECTION_META = {
  concept: { label: "Concept", className: "explanation-section-concept" },
  analogy: { label: "Think of it like...", className: "explanation-section-analogy" },
  example: { label: "For example", className: "explanation-section-example" },
};

function Section({ section }) {
  const meta = SECTION_META[section.type] ?? SECTION_META.concept;

  return (
    <section className={`explanation-section ${meta.className}`}>
      <span className="explanation-section-label">{meta.label}</span>
      <h3>{section.heading}</h3>
      {section.bullets.length > 0 ? (
        <ul>
          {section.bullets.map((bullet, i) => (
            <li key={i}>{bullet}</li>
          ))}
        </ul>
      ) : (
        <p>{section.body}</p>
      )}
    </section>
  );
}

/** Consumes the shape produced by domain/explanationMapper.js's mapExplanation - never raw job JSON. */
function ExplanationRenderer({ explanation }) {
  return (
    <div className="explanation-renderer">
      <p className="explanation-overview">{explanation.overview}</p>
      <div className="explanation-sections">
        {explanation.sections.map((section, i) => (
          <Section key={i} section={section} />
        ))}
      </div>
    </div>
  );
}

export default ExplanationRenderer;
