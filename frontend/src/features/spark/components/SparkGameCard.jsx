/**
 * A quick-break slot: plays the game picked for this slot when the feed was
 * composed (games/catalog.js + games/gamePickers.js). Each game shows its own
 * Skip / Continue, which calls onComplete and moves the feed on.
 */
export default function SparkGameCard({ game, onComplete }) {
  const GameComponent = game.Component;
  return (
    <article className="spark-card spark-card-game">
      <header className="spark-card-meta">
        <span className="spark-card-type">Quick break</span>
        <span className="spark-card-folder">{game.label}</span>
      </header>
      {/* Swipes pass through (taps never count as swipes) unless the game drags itself. */}
      <div className="spark-game-body" data-gesture-ignore={game.ownsGestures || undefined}>
        <GameComponent onComplete={onComplete} />
      </div>
    </article>
  );
}
