/** Instant search over the loaded library - titles and summaries. */
export default function LibrarySearchBox({ value, onChange }) {
  return (
    <div className="library-search">
      <label className="visually-hidden" htmlFor="library-search-input">
        Search your library
      </label>
      <input
        id="library-search-input"
        type="search"
        placeholder="Search your library"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        autoComplete="off"
      />
      {value && (
        <button type="button" className="library-search-clear" aria-label="Clear search text" onClick={() => onChange("")}>
          ×
        </button>
      )}
    </div>
  );
}
