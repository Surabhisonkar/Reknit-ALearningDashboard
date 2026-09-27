import { Link } from "react-router-dom";

import { cx } from "../utils/classNames.js";

export function Button({
  children,
  to,
  onClick,
  secondary = false,
  small = false,
  type = "button",
  className = "",
  ...props
}) {
  const baseClasses = cx("button", secondary && "button-secondary", small && "button-small", className);

  if (to) {
    return (
      <Link className={baseClasses} to={to} {...props}>
        {children}
      </Link>
    );
  }

  return (
    <button className={baseClasses} onClick={onClick} type={type} {...props}>
      {children}
    </button>
  );
}