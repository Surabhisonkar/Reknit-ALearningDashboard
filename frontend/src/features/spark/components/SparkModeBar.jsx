import { useState } from "react";

import { SvgIcon } from "../../../shared/ui";
import { cx } from "../../../shared/utils/classNames.js";
import { SPARK_MODES } from "../modes/sparkModes.js";

const COMING_SOON_MS = 2600;

/** Mode tabs (from the modes registry) plus the folder picker for Folder mode. */
export default function SparkModeBar({ modeId, onModeChange, folders, folder, onFolderChange }) {
  const [notice, setNotice] = useState(null);

  function choose(mode) {
    if (!mode.available) {
      setNotice(`${mode.label} is coming soon.`);
      setTimeout(() => setNotice(null), COMING_SOON_MS);
      return;
    }
    onModeChange(mode.id);
  }

  return (
    <div className="spark-modebar">
      <div className="spark-modes" role="tablist" aria-label="Spark mode">
        {SPARK_MODES.map((mode) => (
          <button
            key={mode.id}
            type="button"
            role="tab"
            aria-selected={mode.id === modeId}
            aria-label={mode.available ? undefined : `${mode.label} (coming soon)`}
            className={cx("spark-mode", mode.id === modeId && "is-active", !mode.available && "is-soon")}
            onClick={() => choose(mode)}
          >
            <SvgIcon name={mode.icon} size={18} />
            <span>{mode.label}</span>
          </button>
        ))}
      </div>
      {modeId === "folder" && (
        <label className="spark-folder-picker">
          <span className="visually-hidden">Folder</span>
          <select value={folder} onChange={(event) => onFolderChange(event.target.value)}>
            <option value="">Choose a folder…</option>
            {folders.map((name) => (
              <option key={name} value={name}>
                {name}
              </option>
            ))}
          </select>
        </label>
      )}
      <p className="spark-notice" role="status" aria-live="polite">
        {notice}
      </p>
    </div>
  );
}
