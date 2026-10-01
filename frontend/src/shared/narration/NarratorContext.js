import { createContext, useContext } from "react";

import { createSilentNarrator } from "./narrators.js";

export const NarratorContext = createContext(createSilentNarrator());

/** The narrator provided above this component (silent if none). */
export function useNarrator() {
  return useContext(NarratorContext);
}
