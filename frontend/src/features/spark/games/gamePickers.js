/**
 * Game-picking strategies. Each has next(previousGameId) -> game | null.
 * Swap the strategy (weighted, unlock-based, ...) without touching the feed.
 */

/** Uniformly random, but never the same game twice in a row (when there's a choice). */
export function createRandomNoRepeatPicker(registry, random = Math.random) {
  return {
    next(previousGameId) {
      const games = registry.all();
      if (games.length === 0) return null;
      const choices = games.length > 1 ? games.filter((g) => g.id !== previousGameId) : games;
      return choices[Math.floor(random() * choices.length)];
    },
  };
}
