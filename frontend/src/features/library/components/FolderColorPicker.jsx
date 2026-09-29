import { AUTO_COLOR, FOLDER_COLORS, folderColorClass } from "../../../shared/constants/folderColors.js";
import { cx } from "../../../shared/utils/classNames.js";

/**
 * Palette swatches as a radio group. With allowAuto, an "Auto" choice comes
 * first; onChange receives a palette key or AUTO_COLOR.
 */
export default function FolderColorPicker({ name, value, onChange, allowAuto = false }) {
  const swatches = allowAuto ? [{ key: AUTO_COLOR, label: "Auto" }, ...FOLDER_COLORS] : FOLDER_COLORS;

  return (
    <fieldset className="folder-editor-colors">
      <legend className="folder-editor-label">Colour</legend>
      <div className="swatch-row">
        {swatches.map((swatch) => (
          <label
            key={swatch.key}
            className={cx(
              "swatch",
              swatch.key === AUTO_COLOR ? "swatch-auto" : folderColorClass(swatch.key),
              value === swatch.key && "is-selected",
            )}
            title={swatch.label}
          >
            <input
              type="radio"
              name={name}
              value={swatch.key}
              checked={value === swatch.key}
              onChange={() => onChange(swatch.key)}
            />
            {swatch.key === AUTO_COLOR ? <span>Auto</span> : <span className="visually-hidden">{swatch.label}</span>}
          </label>
        ))}
      </div>
    </fieldset>
  );
}
