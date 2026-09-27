export function Pill({ children, tone = "neutral", className = "" }) {
  return (
    <span className={`pill pill-${tone} ${className}`.trim()}>
      {children}
    </span>
  );
}