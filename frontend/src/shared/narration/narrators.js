/**
 * Narrator strategy. Every implementation has the same shape:
 *   speak(text): Promise<void>   resolves when finished (or stopped)
 *   stop(): void
 *   isSupported(): boolean
 * The card never knows which one it has. Today: the browser's built-in voice
 * (free, no backend). Later: an AI voice that plays a narration artifact
 * from the backend - a new implementation here, no card changes.
 */

export function createBrowserSpeechNarrator(speech = typeof window !== "undefined" ? window.speechSynthesis : undefined) {
  return {
    isSupported: () => Boolean(speech && typeof SpeechSynthesisUtterance !== "undefined"),
    speak(text) {
      if (!speech || !text) return Promise.resolve();
      speech.cancel();
      return new Promise((resolve) => {
        const utterance = new SpeechSynthesisUtterance(text);
        utterance.rate = 1;
        utterance.onend = () => resolve();
        utterance.onerror = () => resolve();
        speech.speak(utterance);
      });
    },
    stop() {
      speech?.cancel();
    },
  };
}

/** Used where speech isn't available, so callers never need a null check. */
export function createSilentNarrator() {
  return {
    isSupported: () => false,
    speak: () => Promise.resolve(),
    stop: () => {},
  };
}

/** The best narrator this browser supports. */
export function createDefaultNarrator() {
  const browser = createBrowserSpeechNarrator();
  return browser.isSupported() ? browser : createSilentNarrator();
}
