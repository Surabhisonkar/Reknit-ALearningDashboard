import { useState } from "react";

import { Button, Modal } from "../../../shared/ui";
import { folderColorClass } from "../../../shared/constants/folderColors.js";
import { cx } from "../../../shared/utils/classNames.js";
import { AUTO_COLOR, toApiColor } from "../../../shared/constants/folderColors.js";
import FolderColorPicker from "./FolderColorPicker.jsx";

const MAX_NAME_LENGTH = 120;

/**
 * Picks the destination folder for one concept - large rows, current folder
 * marked - or creates a new folder (name + colour) right here and moves the concept into it.
 */
export default function MoveToFolderMenu({ concept, folders, onMove, onCreateAndMove, onClose }) {
  const [creating, setCreating] = useState(false);
  const [name, setName] = useState("");
  const [color, setColor] = useState(AUTO_COLOR);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const options = [
    ...folders.map((f) => ({ id: f.id, name: f.name, colorClass: folderColorClass(f.color) })),
    { id: null, name: "Unfiled", colorClass: "folder-color-none" },
  ];
  const currentId = concept.folderId ?? null;
  const firstChoiceId = options.find((o) => o.id !== currentId)?.id;
  const trimmed = name.trim();

  async function handleCreate(event) {
    event.preventDefault();
    if (!trimmed) return;
    setBusy(true);
    setError(null);
    try {
      await onCreateAndMove(trimmed, toApiColor(color));
    } catch (err) {
      setError(
        err?.body?.code === "DUPLICATE_FOLDER_NAME"
          ? `You already have a folder called “${trimmed}”. Pick it from the list above.`
          : "That didn't work. Check your connection and try again.",
      );
      setBusy(false);
    }
  }

  return (
    <Modal title={`Move “${concept.title}”`} onClose={onClose}>
      <div className="folder-pick-list" role="list">
        {options.map((option) => {
          const current = currentId === option.id;
          return (
            <div role="listitem" key={option.id ?? "unfiled"}>
              <button
                type="button"
                className={cx("folder-pick", option.colorClass, current && "is-current")}
                disabled={current || busy}
                autoFocus={!creating && option.id === firstChoiceId}
                onClick={() => onMove(option.id)}
              >
                <span className="folder-pick-dot" aria-hidden="true" />
                <span>{option.name}</span>
                {current && <span className="folder-pick-note">Current</span>}
              </button>
            </div>
          );
        })}
      </div>

      {creating ? (
        <form className="folder-pick-create" onSubmit={handleCreate}>
          <label className="folder-editor-label" htmlFor="move-new-folder-name">
            New folder name
          </label>
          <input
            id="move-new-folder-name"
            className="modal-input"
            value={name}
            maxLength={MAX_NAME_LENGTH}
            onChange={(event) => {
              setName(event.target.value);
              setError(null);
            }}
            autoFocus
          />
          <FolderColorPicker name="move-new-folder-color" value={color} onChange={setColor} allowAuto />
          {error && (
            <p className="form-error" role="alert">
              {error}
            </p>
          )}
          <div className="modal-actions">
            <Button small type="submit" disabled={!trimmed || busy}>
              Create and move
            </Button>
          </div>
        </form>
      ) : (
        <button
          type="button"
          className="folder-pick folder-pick-new"
          aria-label="New folder"
          title="New folder"
          onClick={() => setCreating(true)}
        >
          <span aria-hidden="true">+</span>
        </button>
      )}
    </Modal>
  );
}
