import { useState } from "react";

import { Button, Modal } from "../../../shared/ui";
import { cx } from "../../../shared/utils/classNames.js";

const UNFILE = "unfile";
const DELETE = "delete";

const plural = (n) => `${n} ${n === 1 ? "concept" : "concepts"}`;

/**
 * Deleting a folder asks what happens to its concepts: keep them (they move to
 * Unfiled - the default) or delete them too. The button always says exactly
 * what will be deleted.
 */
export default function DeleteFolderDialog({ folder, onConfirm, onClose }) {
  const [choice, setChoice] = useState(UNFILE);
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState(null);
  const count = folder.conceptCount;
  const deleteConcepts = count > 0 && choice === DELETE;

  const options = [
    { value: UNFILE, title: "Keep the concepts", detail: `Its ${plural(count)} move to Unfiled.` },
    { value: DELETE, title: "Delete the concepts too", detail: `Its ${plural(count)} are deleted permanently.` },
  ];

  async function handleDelete() {
    setDeleting(true);
    try {
      await onConfirm({ deleteConcepts });
      onClose();
    } catch {
      setError("The folder wasn't deleted. Try again.");
      setDeleting(false);
    }
  }

  return (
    <Modal title={`Delete “${folder.name}”?`} onClose={onClose}>
      {count === 0 ? (
        <p className="modal-text">This folder is empty.</p>
      ) : (
        <fieldset className="choice-list">
          <legend className="visually-hidden">What happens to its concepts</legend>
          {options.map((option) => (
            <label key={option.value} className={cx("choice", choice === option.value && "is-selected", option.value === DELETE && "choice-danger")}>
              <input
                type="radio"
                name="delete-folder-concepts"
                value={option.value}
                checked={choice === option.value}
                onChange={() => setChoice(option.value)}
                autoFocus={option.value === UNFILE}
              />
              <span className="choice-text">
                <strong>{option.title}</strong>
                <span>{option.detail}</span>
              </span>
            </label>
          ))}
        </fieldset>
      )}
      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}
      <div className="modal-actions">
        <Button secondary small onClick={onClose} autoFocus={count === 0}>
          Cancel
        </Button>
        <Button small className="button-danger" onClick={handleDelete} disabled={deleting}>
          {deleteConcepts ? `Delete folder and ${plural(count)}` : "Delete folder"}
        </Button>
      </div>
    </Modal>
  );
}
