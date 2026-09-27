/** Joins truthy class names with a space, skipping falsy ones. `cx("a", cond && "b", "c")`. */
export function cx(...classes) {
  return classes.filter(Boolean).join(" ");
}
