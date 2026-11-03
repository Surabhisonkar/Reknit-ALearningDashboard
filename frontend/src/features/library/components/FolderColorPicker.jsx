import { AUTO_COLOR, FOLDER_COLORS, folderColorClass, folderColorStyle, isCustomColor } from "../../../shared/constants/folderColors.js";
import { cx } from "../../../shared/utils/classNames.js";

const DEFAULT_CUSTOM = "#3a7bd5";

/**
 * Palette swatches as a radio group, plus a "Custom" swatch that opens the
 * system colour picker for any colour. With allowAuto, an "Auto" choice
 * comes first; onChange receives a palette key, a #rrggbb or AUTO_COLOR.
 */
export default function FolderColorPicker({ name, value, onChange, allowAuto = false }) {
  const swatches = allowAuto ? [{ key: AUTO_COLOR, label: "Auto" }, ...FOLDER_COLORS] : FOLDER_COLORS;
  const custom = isCustomColor(value) ? value.toLowerCase() : null;

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
        <label
          className={cx("swatch", "swatch-custom", custom ? folderColorClass(custom) : null, custom && "is-selected")}
          style={folderColorStyle(custom)}
          title={custom ? `Custom colour ${custom}` : "Pick any colour"}
        >
          {/* The native picker gives the full spectrum; choosing a colour selects this swatch. */}
          <input
            type="color"
            name={`${name}-custom`}
            value={custom ?? DEFAULT_CUSTOM}
            onChange={(event) => onChange(event.target.value.toLowerCase())}
            aria-label="Custom colour"
          />
          <span aria-hidden="true">{custom ? "" : "Custom"}</span>
        </label>
      </div>
    </fieldset>
  );
}
