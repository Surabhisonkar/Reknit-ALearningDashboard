import { useEffect, useRef, useState } from "react";

const FOCUSABLE =
  'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

/**
 * Simple accessible modal: overlay + dialog, closes on Escape or overlay click.
 * Focus moves into the dialog when it opens (unless a child already took it
 * with autoFocus) and returns to whatever had focus before, once it closes.
 */
export function Modal({ title, children, onClose }) {
  const dialogRef = useRef(null);
  const pendingRestoreRef = useRef(null);
  // Read during the first render - before any autoFocus child moves focus.
  const [returnTo] = useState(() => (typeof document === "undefined" ? null : document.activeElement));

  useEffect(() => {
    function handleKeyDown(event) {
      if (event.key === "Escape") onClose();
    }
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [onClose]);

  useEffect(() => {
    // A restore scheduled by a cleanup that immediately re-mounted (React StrictMode) is cancelled here.
    clearTimeout(pendingRestoreRef.current);
    const dialog = dialogRef.current;
    if (dialog && !dialog.contains(document.activeElement)) {
      (dialog.querySelector(FOCUSABLE) ?? dialog).focus();
    }
    return () => {
      pendingRestoreRef.current = setTimeout(() => {
        if (returnTo instanceof HTMLElement && returnTo !== document.body && returnTo.isConnected) returnTo.focus();
      });
    };
  }, [returnTo]);

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        ref={dialogRef}
        className="modal-dialog"
        role="dialog"
        aria-modal="true"
        aria-label={title}
        tabIndex={-1}
        onClick={(event) => event.stopPropagation()}
      >
        {title && <h2 className="modal-title">{title}</h2>}
        {children}
      </div>
    </div>
  );
}
