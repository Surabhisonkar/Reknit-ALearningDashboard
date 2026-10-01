import { useCallback, useEffect, useRef, useState } from "react";

import { SvgIcon } from "../../../shared/ui";
import { ProgressDots } from "../../../shared/ui/ProgressDots.jsx";
import { GAME_CHANCE_AFTER_CONCEPT } from "../config/feedMix.js";
import { gameRegistry } from "../games/catalog.js";
import { createRandomNoRepeatPicker } from "../games/gamePickers.js";
import { useRelatedStack } from "../hooks/useRelatedStack.js";
import { useSparkSession } from "../hooks/useSparkSession.js";
import { useSwipeGestures } from "../hooks/useSwipeGestures.js";
import AskSheet from "./AskSheet.jsx";
import RelatedCard from "./RelatedCard.jsx";
import SparkConceptCard from "./SparkConceptCard.jsx";
import SparkGameCard from "./SparkGameCard.jsx";

const TYPING = "input, textarea, select, [contenteditable=true]";

/**
 * One endless Spark session: a single card on stage at a time.
 * Up / down (swipe, wheel, arrow keys, buttons): next / previous card.
 * Left: open - then go deeper into - the related sub-stack. Right: back out.
 */
export default function SparkSession({ accessToken, source }) {
  const [picker] = useState(() => createRandomNoRepeatPicker(gameRegistry));
  const session = useSparkSession(source, { picker, gameChance: GAME_CHANCE_AFTER_CONCEPT });
  const related = useRelatedStack(accessToken);
  const [direction, setDirection] = useState("up");
  const [relatedRevealedId, setRelatedRevealedId] = useState(null); // teaser again on every move
  const [asking, setAsking] = useState(null); // the concept being asked about
  const stageRef = useRef(null);

  const current = session.current;
  const stack = related.stack;

  const goNext = useCallback(() => {
    related.close();
    setDirection("up");
    session.next();
  }, [related, session]);

  const goPrevious = useCallback(() => {
    related.close();
    setDirection("down");
    session.previous();
  }, [related, session]);

  const goLeft = useCallback(() => {
    if (stack) {
      setDirection("left");
      setRelatedRevealedId(null);
      related.deeper();
    } else if (current?.kind === "concept" && session.isRevealed(current.key)) {
      setDirection("left");
      setRelatedRevealedId(null);
      session.hide();
      related.open(current.concept);
    }
  }, [stack, current, session, related]);

  const goRight = useCallback(() => {
    if (!stack) return;
    setDirection("right");
    setRelatedRevealedId(null);
    related.close();
  }, [stack, related]);

  useSwipeGestures(stageRef, {
    onSwipeUp: goNext,
    onSwipeDown: goPrevious,
    onSwipeLeft: goLeft,
    onSwipeRight: goRight,
  });

  useEffect(() => {
    function onKeyDown(event) {
      if (asking || event.target.closest?.(TYPING) || event.target.closest?.("[role=dialog]")) return;
      const actions = { ArrowDown: goNext, PageDown: goNext, ArrowUp: goPrevious, PageUp: goPrevious, ArrowLeft: goLeft, ArrowRight: goRight };
      const action = actions[event.key];
      if (action) {
        event.preventDefault();
        action();
      }
    }
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [asking, goNext, goPrevious, goLeft, goRight]);

  // The stage element is always rendered - even while loading - so the gesture
  // listeners attach on the first render (they attach once, on mount).
  const STATUS_MESSAGES = {
    loading: "Loading your feed…",
    error: "Couldn't load Spark. Try refreshing.",
    empty: "Nothing here yet. Save a few concepts and they'll show up in Spark.",
  };
  const statusMessage = STATUS_MESSAGES[session.status];

  let stageContent;
  let stageKey;
  if (statusMessage) {
    stageKey = session.status;
    stageContent = <div className="spark-state">{statusMessage}</div>;
  } else if (stack) {
    const item = stack.items[stack.index];
    stageKey = `related-${stack.parent.id}-${stack.index}-${stack.status}`;
    stageContent = (
      <div className="spark-related">
        <div className="spark-related-bar">
          <button type="button" className="spark-icon-button" aria-label="Back to the feed" title="Back to the feed" onClick={goRight}>
            <SvgIcon name="chevronLeft" />
          </button>
          <span className="spark-related-title">Related to {stack.parent.title}</span>
          {stack.items.length > 1 && <ProgressDots count={stack.items.length} activeIndex={stack.index} />}
        </div>
        {stack.status === "loading" && <div className="spark-state">Finding related concepts…</div>}
        {(stack.status === "empty" || stack.status === "error") && (
          <div className="spark-state">No related concepts yet. Swipe right to go back.</div>
        )}
        {item && (
          <RelatedCard
            key={item.id}
            accessToken={accessToken}
            related={item}
            revealed={relatedRevealedId === item.id}
            onReveal={() => setRelatedRevealedId(item.id)}
            hasDeeper={stack.index < stack.items.length - 1}
            onDeeper={goLeft}
            onAsk={setAsking}
          />
        )}
      </div>
    );
  } else if (current?.kind === "game") {
    stageKey = current.key;
    stageContent = <SparkGameCard game={current.game} onComplete={goNext} />;
  } else if (current) {
    stageKey = current.key;
    stageContent = (
      <SparkConceptCard
        concept={current.concept}
        revealed={session.isRevealed(current.key)}
        onReveal={() => session.reveal(current.key)}
        onRelated={goLeft}
        relatedLabel="Related concepts"
        onAsk={() => setAsking(current.concept)}
      />
    );
  }

  return (
    <div className="spark-stage" ref={stageRef}>
      <div className={`spark-frame spark-enter-${direction}`} key={stageKey}>
        {stageContent}
      </div>
      {!statusMessage && (
      <nav className="spark-nav" aria-label="Feed navigation">
        <button type="button" className="spark-icon-button" aria-label="Previous card" title="Previous (↑)" onClick={goPrevious} disabled={!session.hasPrevious && !stack}>
          <SvgIcon name="chevronUp" />
        </button>
        <button type="button" className="spark-icon-button" aria-label="Next card" title="Next (↓)" onClick={goNext}>
          <SvgIcon name="chevronDown" />
        </button>
      </nav>
      )}
      {asking && <AskSheet accessToken={accessToken} concept={asking} onClose={() => setAsking(null)} />}
    </div>
  );
}
