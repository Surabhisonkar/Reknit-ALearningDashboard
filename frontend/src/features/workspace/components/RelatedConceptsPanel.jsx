import { Link } from "react-router-dom";

/**
 * "Related concepts" - a small row of cards linking to concepts the user
 * already saved that connect to this one. Renders nothing at all when
 * there are none: no heading, no empty box.
 */
function RelatedConceptsPanel({ related }) {
  if (!related || related.length === 0) return null;

  return (
    <section className="related-concepts" aria-labelledby="related-concepts-heading">
      <h2 id="related-concepts-heading" className="related-concepts-heading">
        Connects to
      </h2>
      <ul className="related-concepts-row">
        {related.map((concept) => (
          <li key={concept.id}>
            <Link className="related-concept-card" to={`/workspace?conceptId=${encodeURIComponent(concept.id)}`}>
              <strong>{concept.title}</strong>
              {concept.summary && <span>{concept.summary}</span>}
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}

export default RelatedConceptsPanel;
