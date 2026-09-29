import { Link } from "react-router-dom";

import { OverflowMenu, Pill } from "../../../shared/ui";
import { folderColorClass } from "../../../shared/constants/folderColors.js";
import { visualizationTypeLabel } from "../../../shared/constants/visualizationTypes.js";
import { cx } from "../../../shared/utils/classNames.js";

/** One saved concept. The top band is its folder's colour, so topics read at a glance. */
export default function LibraryConceptCard({ concept, onMove, onDelete }) {
  return (
    <article className={cx("library-card", folderColorClass(concept.folderColor))}>
      <span className="library-card-band" aria-hidden="true" />
      <div className="library-card-meta">
        <Pill tone="neutral">{visualizationTypeLabel(concept.visualizationType)}</Pill>
        <span className="library-card-folder">{concept.folderId ? concept.folder : "Unfiled"}</span>
      </div>
      <h3>
        <Link className="library-card-link" to={`/workspace?conceptId=${concept.id}`}>
          {concept.title}
        </Link>
      </h3>
      <p className="library-card-summary">{concept.summary}</p>
      <div className="library-card-footer">
        <Link className="library-card-open" to={`/workspace?conceptId=${concept.id}`} tabIndex={-1} aria-hidden="true">
          Open
        </Link>
        <OverflowMenu
          label={`Actions for ${concept.title}`}
          items={[
            { label: "Move to folder", onSelect: () => onMove(concept) },
            { label: "Delete", onSelect: () => onDelete(concept), danger: true },
          ]}
        />
      </div>
    </article>
  );
}
