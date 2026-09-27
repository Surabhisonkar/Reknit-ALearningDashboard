import { useEffect, useId, useRef, useState } from "react";

import { cx } from "../utils/classNames.js";

/**
 * A small "⋯" button that reveals secondary actions - keeps rarely-used,
 * consequential actions (delete, regenerate) out of the permanent chrome.
 * Closes on outside click, Escape, or after an item runs.
 *
 * @param {{ label: string, onSelect: () => void, danger?: boolean, disabled?: boolean }[]} items
 * @param {string} [label] accessible name for the trigger button
 */
export function OverflowMenu({ items, label = "More actions", className = "" }) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef(null);
  const menuId = useId();

  useEffect(() => {
    if (!open) return undefined;
    function handlePointerDown(event) {
      if (rootRef.current && !rootRef.current.contains(event.target)) setOpen(false);
    }
    function handleKeyDown(event) {
      if (event.key === "Escape") setOpen(false);
    }
    document.addEventListener("pointerdown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("pointerdown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [open]);

  function run(item) {
    setOpen(false);
    item.onSelect();
  }

  return (
    <div className={cx("overflow-menu", className)} ref={rootRef}>
      <button
        type="button"
        className="overflow-menu-trigger"
        aria-label={label}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-controls={open ? menuId : undefined}
        onClick={() => setOpen((o) => !o)}
      >
        <span aria-hidden="true">⋯</span>
      </button>
      {open && (
        <div className="overflow-menu-list" role="menu" id={menuId}>
          {items.map((item) => (
            <button
              key={item.label}
              type="button"
              role="menuitem"
              className={cx("overflow-menu-item", item.danger && "overflow-menu-item-danger")}
              disabled={item.disabled}
              onClick={() => run(item)}
            >
              {item.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
