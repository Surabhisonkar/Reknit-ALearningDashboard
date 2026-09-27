export function Tabs({ items, activeItem, onSelect, className = "" }) {
  return (
    <div className={`tab-group ${className}`.trim()}>
      {items.map((item) => (
        <button
          key={item}
          type="button"
          className={`tab ${activeItem === item ? "active" : ""}`}
          onClick={() => onSelect(item)}
        >
          {item}
        </button>
      ))}
    </div>
  );
}