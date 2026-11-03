import { useEffect, useId, useRef, useState } from "react";

import { cx } from "../utils/classNames.js";

/**
 * A primary button with a dropdown on its right that picks what the button
 * will do. `options` are { value, label, hint }; the chosen one is shown in
 * the button's label and stays chosen until changed.
 */
export function SplitButton({ children, onClick, options, value, onChange, disabled = false, menuLabel }) {
  const [open, setOpen] = useState(false);
  const [dropUp, setDropUp] = useState(false);
  const rootRef = useRef(null);
  const menuId = useId();

  useEffect(() => {
    if (!open) return undefined;
    function onPointerDown(event) {
      if (!rootRef.current?.contains(event.target)) setOpen(false);
    }
    function onKeyDown(event) {
      if (event.key === "Escape") setOpen(false);
    }
    document.addEventListener("pointerdown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("pointerdown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [open]);

  function choose(next) {
    onChange(next);
    setOpen(false);
  }

  return (
    <div className="split-button" ref={rootRef}>
      <button type="button" className="button split-button-main" onClick={onClick} disabled={disabled}>
        {children}
      </button>
      <button
        type="button"
        className={cx("button", "split-button-toggle", open && "split-button-toggle-open")}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-controls={menuId}
        aria-label={menuLabel}
        onClick={() => {
          // Open upward when the button sits too near the bottom of the window for the menu to fit.
          const rect = rootRef.current.getBoundingClientRect();
          setDropUp(window.innerHeight - rect.bottom < 360 && rect.top > window.innerHeight - rect.bottom);
          setOpen((o) => !o);
        }}
        disabled={disabled}
      >
        <svg viewBox="0 0 12 12" width="12" height="12" aria-hidden="true">
          <path d="M2.5 4.5 6 8l3.5-3.5" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </button>

      {open && (
        <div className={cx("split-button-menu", dropUp && "split-button-menu-up")} role="menu" id={menuId} aria-label={menuLabel}>
          {options.map((option) => (
            <button
              key={option.value}
              type="button"
              role="menuitemradio"
              aria-checked={option.value === value}
              className={cx("split-button-option", option.value === value && "split-button-option-active")}
              onClick={() => choose(option.value)}
            >
              <strong>{option.label}</strong>
              {option.hint && <span>{option.hint}</span>}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
