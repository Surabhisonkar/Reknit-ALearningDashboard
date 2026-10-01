import { useCallback, useEffect, useState } from "react";

import { useNarrator } from "./NarratorContext.js";

const MUTE_KEY = "narration-muted";

function readMuted() {
  try {
    return window.localStorage.getItem(MUTE_KEY) === "1";
  } catch {
    return false;
  }
}

function writeMuted(muted) {
  try {
    window.localStorage.setItem(MUTE_KEY, muted ? "1" : "0");
  } catch {
    // Storage can be unavailable (private mode); muting still works for this visit.
  }
}

/**
 * Plays `text` through the provided narrator. The mute choice is remembered
 * across visits. Speech stops when the component unmounts (e.g. the card
 * changes), so two cards never talk over each other.
 */
export function useNarration() {
  const narrator = useNarrator();
  const [muted, setMuted] = useState(readMuted);
  const [speaking, setSpeaking] = useState(false);

  useEffect(() => () => narrator.stop(), [narrator]);

  const speak = useCallback(
    (text) => {
      if (!text) return;
      setSpeaking(true);
      narrator.speak(text).finally(() => setSpeaking(false));
    },
    [narrator],
  );

  const stop = useCallback(() => {
    narrator.stop();
    setSpeaking(false);
  }, [narrator]);

  const toggleMuted = useCallback(() => {
    setMuted((prev) => {
      const next = !prev;
      writeMuted(next);
      if (next) narrator.stop();
      return next;
    });
  }, [narrator]);

  return { supported: narrator.isSupported(), muted, speaking, speak, stop, toggleMuted };
}
