/**
 * Registry of Spark mini-games. A game is a plain descriptor:
 *   { id, label, Component, ownsGestures? }   where Component takes
 *   { onComplete } and must show its own Skip / Continue that calls it (the
 *   card adds no second one). Set ownsGestures: true for a game that uses
 *   drags or swipes itself; otherwise swiping over it moves the feed on.
 * Adding a game = write its component, add one register() line in catalog.js.
 * Nothing that shows or picks games changes.
 */
export class GameRegistry {
  #games = new Map();

  register(game) {
    if (!game?.id || !game.Component) throw new Error("A game needs an id and a Component.");
    if (this.#games.has(game.id)) throw new Error(`Game "${game.id}" is already registered.`);
    this.#games.set(game.id, Object.freeze({ ...game }));
    return this;
  }

  all() {
    return [...this.#games.values()];
  }

  byId(id) {
    return this.#games.get(id) ?? null;
  }
}
