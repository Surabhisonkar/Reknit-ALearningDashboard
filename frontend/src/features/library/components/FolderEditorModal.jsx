import { useState } from "react";

import { Button, Modal } from "../../../shared/ui";
import { AUTO_COLOR, toApiColor } from "../../../shared/constants/folderColors.js";
import FolderColorPicker from "./FolderColorPicker.jsx";

const MAX_NAME_LENGTH = 120;

/**
 * Create a folder (name + optional colour; "Auto" lets the server pick) or
 * edit one (rename and/or recolour). Shows the server's duplicate-name error inline.
 */
export default function FolderEditorModal({ folder, onSubmit, onClose }) {
  const editing = Boolean(folder);
  const [name, setName] = useState(folder?.name ?? "");
  const [color, setColor] = useState(folder?.color ?? AUTO_COLOR);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  const trimmed = name.trim();
  const unchanged = editing && trimmed === folder.name && color === folder.color;

  async function handleSubmit(event) {
    event.preventDefault();
    if (!trimmed || unchanged) return;
    setSaving(true);
    setError(null);
    try {
      await onSubmit({ name: trimmed, color: toApiColor(color) });
      onClose();
    } catch (err) {
      setError(
        err?.body?.code === "DUPLICATE_FOLDER_NAME"
          ? `You already have a folder called “${trimmed}”.`
          : "That didn't save. Check your connection and try again.",
      );
      setSaving(false);
    }
  }

  return (
    <Modal title={editing ? "Edit folder" : "New folder"} onClose={onClose}>
      <form className="folder-editor" onSubmit={handleSubmit}>
        <label className="folder-editor-label" htmlFor="folder-name-input">
          Name
        </label>
        <input
          id="folder-name-input"
          className="modal-input"
          value={name}
          maxLength={MAX_NAME_LENGTH}
          onChange={(event) => {
            setName(event.target.value);
            setError(null);
          }}
          autoFocus
        />
        <FolderColorPicker name="folder-color" value={color} onChange={setColor} allowAuto={!editing} />
        {error && (
          <p className="form-error" role="alert">
            {error}
          </p>
        )}
        <div className="modal-actions">
          <Button secondary small onClick={onClose}>
            Cancel
          </Button>
          <Button small type="submit" disabled={!trimmed || unchanged || saving}>
            {editing ? "Save changes" : "Create folder"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
