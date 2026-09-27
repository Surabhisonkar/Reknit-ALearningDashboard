import { Link } from "react-router-dom";

import { useConceptList } from "../features/workspace";
import { Button } from "../shared/ui";
import { Pill } from "../shared/ui/Pill";

const TYPE_LABELS = { mind_map: "Mind Map", diagram: "Diagram", animation: "Animation", image: "Image" };

function LibraryPage() {
  const { concepts, status, remove } = useConceptList();

  if (status === "loading") {
    return (
      <main className="page-width" style={{ padding: "4rem 0", textAlign: "center" }}>
        <p>Loading your library...</p>
      </main>
    );
  }

  if (status === "error") {
    return (
      <main className="page-width" style={{ padding: "4rem 0", textAlign: "center" }}>
        <h1>Couldn't load your library</h1>
        <p>Please try refreshing the page.</p>
      </main>
    );
  }

  return (
    <main className="page-width" style={{ padding: "3rem 0" }}>
      <h1>Library</h1>

      {concepts.length === 0 ? (
        <div style={{ textAlign: "center", padding: "3rem 0" }}>
          <p>You haven't saved any concepts yet.</p>
          <Button to="/create">Create your first concept</Button>
        </div>
      ) : (
        <div className="library-grid">
          {concepts.map((concept) => (
            <article key={concept.id} className="library-card">
              <Pill tone="teal">{TYPE_LABELS[concept.visualizationType] ?? concept.visualizationType}</Pill>
              <h3>{concept.title}</h3>
              <p>{concept.summary}</p>
              <div className="library-card-footer">
                <Link to={`/workspace?conceptId=${concept.id}`}>Open</Link>
                <button type="button" onClick={() => remove(concept.id)}>
                  Delete
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </main>
  );
}

export default LibraryPage;
