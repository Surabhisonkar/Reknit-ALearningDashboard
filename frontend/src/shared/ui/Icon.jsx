export function Icon({ children, className = "" }) {
  return (
    <span className={`icon ${className}`.trim()} aria-hidden="true">
      {children}
    </span>
  );
}