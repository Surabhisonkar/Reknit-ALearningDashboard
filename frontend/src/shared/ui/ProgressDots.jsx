import { cx } from "../utils/classNames.js";

/**
 * The dot strip first built for the animation renderer's scene progress,
 * extracted so other "which one of N am I looking at" UIs (concept
 * versions) reuse the exact same markup and CSS classes instead of
 * inventing a new component. Purely presentational: the caller owns the
 * active index.
 *
 * @param {number} count          how many dots
 * @param {number} activeIndex    0-based active dot
 * @param {(index:number)=>void} onSelect
 * @param {string} ariaLabel      label for the whole strip (e.g. "Scenes", "Versions")
 * @param {(index:number)=>string} [getKey]   stable React key per dot (defaults to index)
 * @param {(index:number)=>string} [getLabel] accessible label per dot (optional)
 */
export function ProgressDots({ count, activeIndex, onSelect, ariaLabel, getKey, getLabel, className = "" }) {
  return (
    <div className={cx("animation-progress", className)} role="tablist" aria-label={ariaLabel}>
      {Array.from({ length: count }, (_, i) => (
        <button
          key={getKey ? getKey(i) : i}
          type="button"
          role="tab"
          aria-selected={i === activeIndex}
          aria-label={getLabel ? getLabel(i) : undefined}
          title={getLabel ? getLabel(i) : undefined}
          className={i === activeIndex ? "animation-dot active" : "animation-dot"}
          onClick={() => onSelect(i)}
        />
      ))}
    </div>
  );
}
