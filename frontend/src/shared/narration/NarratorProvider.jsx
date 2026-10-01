import { useState } from "react";

import { NarratorContext } from "./NarratorContext.js";
import { createDefaultNarrator } from "./narrators.js";

/** Supplies one narrator to everything below it; pass `narrator` to swap the implementation. */
export default function NarratorProvider({ narrator, children }) {
  const [fallback] = useState(createDefaultNarrator);
  return <NarratorContext.Provider value={narrator ?? fallback}>{children}</NarratorContext.Provider>;
}
