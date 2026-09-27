import { useState } from "react";

import { Pill } from "../../../shared/ui";
import { useInView } from "../../../shared/hooks/useInView.js";
import QuickMatchGame from "./QuickMatchGame.jsx";
import TapDashGame from "./TapDashGame.jsx";

const GAMES = [
  { id: "match", label: "Quick Match", Component: QuickMatchGame },
  { id: "dash", label: "Tap Dash", Component: TapDashGame },
];

/**
 * Picks and plays one round. Lives in its own component (rather than
 * inline in SparkGameCard) so the random pick can use a lazy useState
 * initializer - the sanctioned way to call an impure function (Math.random)
 * as part of render, since the initializer only ever runs once per mount.
 * Remounting this (via `key`, from the parent) is what makes a "new"
 * round happen each time the slide re-enters the viewport.
 */
function SparkGameRound({ onComplete }) {
  const [game] = useState(() => GAMES[Math.floor(Math.random() * GAMES.length)]);
  const GameComponent = game.Component;

  return (
    <>
      <div className="spark-slide-header">
        <Pill tone="yellow">QUICK BREAK</Pill>
        <Pill tone="coral">{game.label}</Pill>
      </div>
      <div className="spark-slide-body spark-slide-body-game">
        <GameComponent onComplete={onComplete} />
      </div>
    </>
  );
}

/**
 * A reel slide interleaved between animations - a short mini-game to keep
 * attention, the way LinkedIn's puzzle games break up a feed. A fresh
 * game is picked each time the slide scrolls into view (see
 * SparkGameRound); finishing (or skipping) it auto-advances the reel to
 * the next slide, same as swiping up in a real Reels-style feed.
 */
function SparkGameCard() {
  const { ref, enterCount } = useInView();
  const hasEntered = enterCount > 0;

  function advance() {
    ref.current?.nextElementSibling?.scrollIntoView({ behavior: "smooth", block: "start" });
  }

  return (
    <section className="spark-slide spark-slide-game" ref={ref}>
      {hasEntered ? (
        <SparkGameRound key={enterCount} onComplete={advance} />
      ) : (
        <>
          <div className="spark-slide-header">
            <Pill tone="yellow">QUICK BREAK</Pill>
          </div>
          <div className="spark-slide-body spark-slide-body-game">
            <div className="spark-slide-placeholder" aria-hidden="true" />
          </div>
        </>
      )}
    </section>
  );
}

export default SparkGameCard;
