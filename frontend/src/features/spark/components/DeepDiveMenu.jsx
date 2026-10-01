import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";

import { SvgIcon } from "../../../shared/ui";

/**
 * The deep-dive icon on a revealed card. It opens two icon-only choices:
 * open this concept's page, or ask the AI about it.
 */
export default function DeepDiveMenu({ concept, onAsk }) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef(null);
  const triggerRef = useRef(null);
  const navigate = useNavigate();

  useEffect(() => {
    if (!open) return undefined;
    function onPointerDown(event) {
      if (!rootRef.current?.contains(event.target)) setOpen(false);
    }
    function onKeyDown(event) {
      if (event.key === "Escape") {
        setOpen(false);
        triggerRef.current?.focus();
      }
    }
    document.addEventListener("pointerdown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("pointerdown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [open]);

  function choose(action) {
    setOpen(false);
    triggerRef.current?.focus();
    action();
  }

  return (
    <div className="deep-dive" ref={rootRef} data-gesture-ignore>
      <button
        ref={triggerRef}
        type="button"
        className="spark-icon-button spark-icon-button-accent"
        aria-label="Deep dive"
        title="Deep dive"
        aria-haspopup="true"
        aria-expanded={open}
        onClick={() => setOpen((o) => !o)}
      >
        <SvgIcon name="sparkle" />
      </button>
      {open && (
        <div className="deep-dive-choices" role="group" aria-label="Deep dive">
          <button
            type="button"
            className="spark-icon-button"
            aria-label="Open in Library"
            title="Open in Library"
            onClick={() => choose(() => navigate(`/workspace?conceptId=${concept.id}`))}
          >
            <SvgIcon name="book" />
          </button>
          <button type="button" className="spark-icon-button" aria-label="Ask the AI" title="Ask the AI" onClick={() => choose(onAsk)} autoFocus>
            <SvgIcon name="chat" />
          </button>
        </div>
      )}
    </div>
  );
}
