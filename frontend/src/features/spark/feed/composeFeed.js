/**
 * Turns a page of concepts into feed items, inserting game slots at random:
 * after each concept, a game follows with probability `gameChance`. Never
 * first, never two in a row. Pure: randomness and game choice are injected.
 *
 * @returns {{ items, nextSeq, lastGameId }}
 */
export function composeFeed(concepts, { picker, gameChance, random, startSeq = 0, lastGameId = null }) {
  const items = [];
  let seq = startSeq;
  let previousGame = lastGameId;

  for (const concept of concepts) {
    items.push({ kind: "concept", key: `c-${seq++}-${concept.id}`, concept });
    if (random() < gameChance) {
      const game = picker.next(previousGame);
      if (game) {
        items.push({ kind: "game", key: `g-${seq++}-${game.id}`, game });
        previousGame = game.id;
      }
    }
  }
  return { items, nextSeq: seq, lastGameId: previousGame };
}
