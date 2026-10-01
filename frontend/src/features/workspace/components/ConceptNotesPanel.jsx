import { SvgIcon } from "../../../shared/ui";

const dateFormat = new Intl.DateTimeFormat(undefined, { day: "numeric", month: "short" });

/** Saved notes under the visual. Collapsed by default; renders nothing when there are none. */
export default function ConceptNotesPanel({ notes, onDelete }) {
  if (notes.length === 0) return null;
  return (
    <details className="notes-panel page-width">
      <summary>
        <SvgIcon name="note" size={18} />
        <span>Notes</span>
        <span className="notes-count">{notes.length}</span>
      </summary>
      <ul className="notes-list">
        {notes.map((note) => (
          <li key={note.id} className="note">
            <p>{note.content}</p>
            <div className="note-meta">
              <span>From a Spark chat · {dateFormat.format(new Date(note.createdAt))}</span>
              <button type="button" className="note-delete" aria-label="Delete note" title="Delete note" onClick={() => onDelete(note.id)}>
                <SvgIcon name="trash" size={16} />
              </button>
            </div>
          </li>
        ))}
      </ul>
    </details>
  );
}
