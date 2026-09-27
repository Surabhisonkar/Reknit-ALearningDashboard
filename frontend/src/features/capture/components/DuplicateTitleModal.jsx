import { useState } from "react";

import { Button, Modal } from "../../../shared/ui";

/**
 * Shown when Save hits an existing title. Nothing has been written yet -
 * each choice re-sends the save with a different resolution, and closing
 * the modal simply cancels (the draft stays on screen).
 * Keyed by `title` at the call site, so the input resets if a rename
 * collides again.
 */
function DuplicateTitleModal({ title, busy, onRename, onReplace, onKeepBoth, onClose }) {
  const [renameValue, setRenameValue] = useState(title);
  const trimmed = renameValue.trim();

  return (
    <Modal title="A concept with this name already exists" onClose={busy ? () => {} : onClose}>
      <p>You already have a saved concept called “{title}”. What would you like to do?</p>
      <input
        className="modal-input"
        value={renameValue}
        onChange={(event) => setRenameValue(event.target.value)}
        placeholder="New title"
        aria-label="New title"
      />
      <div className="modal-actions">
        <Button secondary onClick={onKeepBoth} disabled={busy}>
          Keep both
        </Button>
        <Button secondary onClick={onReplace} disabled={busy}>
          Replace the old one
        </Button>
        <Button onClick={() => onRename(trimmed)} disabled={busy || !trimmed || trimmed === title}>
          Save with new name
        </Button>
      </div>
    </Modal>
  );
}

export default DuplicateTitleModal;
