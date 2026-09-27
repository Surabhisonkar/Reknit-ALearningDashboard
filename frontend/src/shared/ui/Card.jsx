export function Card({ children, className = "", variant = "default", ...props }) {
  return (
    <div className={`showcase tactile-panel ${variant} ${className}`.trim()} {...props}>
      {children}
    </div>
  );
}