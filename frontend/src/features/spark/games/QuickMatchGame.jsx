import { useEffect, useMemo, useState } from "react";

const SYMBOL_SETS = [
  ["\u{1F9E0}", "\u{1F4A1}", "\u{1F52C}", "\u{1F9E9}"], // brain, bulb, microscope, puzzle
  ["\u{1F680}", "\u{1F30D}", "⭐", "\u{1F52D}"], // rocket, globe, star, telescope
  ["\u{1F3A8}", "\u{1F3B5}", "\u{1F4D0}", "♾️"], // palette, music, ruler, infinity
];

const ROUND_SECONDS = 20;

function buildDeck() {
  const symbols = SYMBOL_SETS[Math.floor(Math.random() * SYMBOL_SETS.length)];
  const deck = symbols.flatMap((symbol, pairIndex) => [
    { id: `${pairIndex}-a`, symbol, matched: false },
    { id: `${pairIndex}-b`, symbol, matched: false },
  ]);
  for (let i = deck.length - 1; i > 0; i -= 1) {
    const j = Math.floor(Math.random() * (i + 1));
    [deck[i], deck[j]] = [deck[j], deck[i]];
  }
  return deck;
}

/** A quick memory-match break between reel cards - built to finish (win or lose) inside ~20 seconds. */
function QuickMatchGame({ onComplete }) {
  const [deck] = useState(buildDeck);
  const [flipped, setFlipped] = useState([]); // up to 2 card ids currently face-up
  const [matched, setMatched] = useState(() => new Set());
  const [secondsLeft, setSecondsLeft] = useState(ROUND_SECONDS);
  const [locked, setLocked] = useState(false); // true while a mismatched pair is being shown before flipping back
  const finished = matched.size === deck.length;

  useEffect(() => {
    if (finished || secondsLeft <= 0) return undefined;
    const timer = setInterval(() => setSecondsLeft((s) => s - 1), 1000);
    return () => clearInterval(timer);
  }, [finished, secondsLeft]);

  const won = finished;
  const timedOut = !finished && secondsLeft <= 0;

  function handleFlip(card) {
    if (locked || card.matched || flipped.includes(card.id) || flipped.length === 2 || timedOut) return;

    const next = [...flipped, card.id];
    setFlipped(next);

    if (next.length === 2) {
      const [firstId, secondId] = next;
      const first = deck.find((c) => c.id === firstId);
      const second = deck.find((c) => c.id === secondId);

      if (first.symbol === second.symbol) {
        setMatched((prev) => new Set(prev).add(firstId).add(secondId));
        setFlipped([]);
      } else {
        setLocked(true);
        setTimeout(() => {
          setFlipped([]);
          setLocked(false);
        }, 700);
      }
    }
  }

  const resultLabel = useMemo(() => {
    if (won) return "Nice - fully matched!";
    if (timedOut) return "Time's up - on to the next one.";
    return null;
  }, [won, timedOut]);

  return (
    <div className="spark-game spark-game-match">
      <p className="spark-game-instructions">Quick break: find every pair.</p>

      <div className="spark-game-grid">
        {deck.map((card) => {
          const isFaceUp = flipped.includes(card.id) || matched.has(card.id) || timedOut;
          return (
            <button
              key={card.id}
              type="button"
              className={`spark-match-card ${isFaceUp ? "face-up" : ""} ${matched.has(card.id) ? "is-matched" : ""}`}
              onClick={() => handleFlip(card)}
              disabled={matched.has(card.id) || timedOut}
              aria-label={isFaceUp ? card.symbol : "Hidden card"}
            >
              {isFaceUp ? card.symbol : ""}
            </button>
          );
        })}
      </div>

      <div className="spark-game-footer">
        {resultLabel ? (
          <>
            <span>{resultLabel}</span>
            <button type="button" className="button button-small" onClick={onComplete}>
              Continue
            </button>
          </>
        ) : (
          <>
            <span>{secondsLeft}s left</span>
            <button type="button" className="button button-secondary button-small" onClick={onComplete}>
              Skip
            </button>
          </>
        )}
      </div>
    </div>
  );
}

export default QuickMatchGame;
