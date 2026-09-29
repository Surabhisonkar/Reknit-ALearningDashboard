import { useState } from "react";

import { folderColorClass } from "../../../shared/constants/folderColors.js";
import { useRowOverflow } from "../../../shared/hooks/useRowOverflow.js";
import { cx } from "../../../shared/utils/classNames.js";
import { ALL_FOLDERS, UNFILED } from "../../../domain/conceptSearch.js";

function FolderTile({ tileKey, label, count, colorClass, active, hidden, onSelect }) {
  return (
    <button
      type="button"
      data-overflow-key={tileKey}
      className={cx("folder-tile", colorClass, active && "is-active")}
      aria-pressed={active}
      inert={hidden}
      onClick={onSelect}
    >
      <span className="folder-tile-bar" aria-hidden="true" />
      <span className="folder-tile-name">{label}</span>
      <span className="folder-tile-count">{count}</span>
    </button>
  );
}

/**
 * The Library's "playlists": All, each folder in its colour, Unfiled.
 * On desktop they sit in one row with "N more" to expand into the full,
 * wrapped view; on small screens the row scrolls sideways instead.
 * New folder stays visible in both states.
 */
export default function FolderStrip({ folders, totalCount, unfiledCount, activeFolder, onSelect, onCreate }) {
  const [expanded, setExpanded] = useState(false);

  const tiles = [
    { key: ALL_FOLDERS, label: "All concepts", count: totalCount, colorClass: "folder-color-all" },
    ...folders.map((f) => ({ key: f.id, label: f.name, count: f.conceptCount, colorClass: folderColorClass(f.color) })),
    ...(unfiledCount > 0 ? [{ key: UNFILED, label: "Unfiled", count: unfiledCount, colorClass: "folder-color-none" }] : []),
  ];
  const signature = tiles.map((t) => `${t.key}:${t.label}`).join("|");
  const { rowRef, observeRef, hiddenKeys } = useRowOverflow(signature);
  const hiddenCount = hiddenKeys.size;

  return (
    <nav className={cx("folder-strip", expanded && "is-expanded")} aria-label="Folders" ref={observeRef}>
      <div className="folder-strip-tiles" ref={rowRef} id="folder-strip-tiles">
        {tiles.map((t) => (
          <FolderTile
            key={t.key}
            tileKey={t.key}
            label={t.label}
            count={t.count}
            colorClass={t.colorClass}
            active={activeFolder === t.key}
            hidden={!expanded && hiddenKeys.has(t.key)}
            onSelect={() => onSelect(t.key)}
          />
        ))}
      </div>
      <div className="folder-strip-actions">
        <button type="button" className="folder-add" aria-label="New folder" title="New folder" onClick={onCreate}>
          <span aria-hidden="true">+</span>
        </button>
        {(hiddenCount > 0 || expanded) && (
          <button
            type="button"
            className="folder-more"
            aria-expanded={expanded}
            aria-controls="folder-strip-tiles"
            onClick={() => setExpanded((e) => !e)}
          >
            {expanded ? "Show less" : `${hiddenCount} more`}
          </button>
        )}
      </div>
    </nav>
  );
}
