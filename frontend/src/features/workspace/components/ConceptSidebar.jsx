import { useMemo, useState } from "react";
import { Link } from "react-router-dom";

import { folderColorClass, folderColorStyle } from "../../../shared/constants/folderColors.js";
import { SvgIcon } from "../../../shared/ui";
import { useFolders } from "../../library/hooks/useFolders.js";
import { useConceptList } from "../hooks/useConceptList.js";

const UNFILED_KEY = "__unfiled";

/** Groups concepts under their folders (in folder order), with unfiled ones last. */
function groupConcepts(concepts, folders) {
  const byFolder = new Map(folders.map((f) => [f.id, []]));
  const unfiled = [];
  for (const concept of concepts) {
    (byFolder.get(concept.folderId) ?? unfiled).push(concept);
  }
  const groups = folders.map((f) => ({ key: f.id, name: f.name, color: f.color, concepts: byFolder.get(f.id) }));
  if (unfiled.length) groups.push({ key: UNFILED_KEY, name: "Unfiled", color: null, concepts: unfiled });
  return groups;
}

/**
 * The library beside an open concept: folders that fold open, the concepts in
 * each, and a search box. The open concept is highlighted and its folder
 * starts open; picking another concept opens it in place.
 */
export function ConceptSidebar({ activeId, onNavigate }) {
  const { concepts, status } = useConceptList();
  const { folders } = useFolders();
  const [query, setQuery] = useState("");
  const [toggled, setToggled] = useState({});

  const activeFolderKey = useMemo(() => {
    const active = concepts.find((c) => c.id === activeId);
    if (!active) return null;
    return folders.some((f) => f.id === active.folderId) ? active.folderId : UNFILED_KEY;
  }, [concepts, folders, activeId]);

  const needle = query.trim().toLowerCase();
  const groups = groupConcepts(
    needle ? concepts.filter((c) => c.title.toLowerCase().includes(needle)) : concepts,
    folders
  ).filter((g) => !needle || g.concepts.length > 0);

  // A folder is open if the user opened it, or by default when it holds the open concept or a search hit.
  const isOpen = (key) => toggled[key] ?? (Boolean(needle) || key === activeFolderKey);

  return (
    <nav className="concept-sidebar" aria-label="Your concepts">
      <Link className="concept-sidebar-home" to="/library" onClick={onNavigate}>
        <SvgIcon name="book" size={18} /> Library
      </Link>

      <label className="concept-sidebar-search">
        <span className="visually-hidden">Search concepts</span>
        <input type="search" placeholder="Search concepts..." value={query} onChange={(e) => setQuery(e.target.value)} />
      </label>

      <p className="concept-sidebar-heading">FOLDERS</p>

      {status === "loading" && <p className="concept-sidebar-empty">Loading...</p>}
      {status === "error" && <p className="concept-sidebar-empty">Couldn't load your concepts.</p>}
      {status === "ready" && groups.length === 0 && (
        <p className="concept-sidebar-empty">{needle ? "No concepts match." : "No concepts yet."}</p>
      )}

      <ul className="concept-sidebar-groups">
        {groups.map((group) => {
          const open = isOpen(group.key);
          const holdsActive = group.key === activeFolderKey;
          return (
            <li key={group.key} className={folderColorClass(group.color)} style={folderColorStyle(group.color)}>
              <button
                type="button"
                className={`concept-sidebar-folder ${holdsActive ? "concept-sidebar-folder-active" : ""}`}
                aria-expanded={open}
                onClick={() => setToggled((t) => ({ ...t, [group.key]: !open }))}
              >
                <span className="concept-sidebar-folder-icon">
                  <SvgIcon name="folder" size={17} />
                </span>
                <span className="concept-sidebar-label">{group.name}</span>
                <span className="concept-sidebar-count">{group.concepts.length}</span>
                <svg className="concept-sidebar-chevron" viewBox="0 0 12 12" width="12" height="12" aria-hidden="true">
                  <path d="M4.5 2.5 8 6l-3.5 3.5" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              </button>
              {open && (
                <ul className="concept-sidebar-concepts">
                  {group.concepts.map((concept) => {
                    const active = concept.id === activeId;
                    return (
                      <li key={concept.id}>
                        <Link
                          to={`/workspace?conceptId=${encodeURIComponent(concept.id)}`}
                          className={`concept-sidebar-concept ${active ? "concept-sidebar-concept-active" : ""}`}
                          aria-current={active ? "page" : undefined}
                          onClick={onNavigate}
                        >
                          <SvgIcon name="note" size={16} />
                          <span className="concept-sidebar-label">{concept.title}</span>
                          {active && (
                            <svg className="concept-sidebar-check" viewBox="0 0 16 16" width="16" height="16" aria-hidden="true">
                              <circle cx="8" cy="8" r="7" fill="none" stroke="currentColor" strokeWidth="1.4" />
                              <path d="M5 8.2 7 10l4-4.2" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
                            </svg>
                          )}
                        </Link>
                      </li>
                    );
                  })}
                </ul>
              )}
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
