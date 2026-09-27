import { useEffect, useRef, useState } from "react";

const WIDTH = 300;
const HEIGHT = 380;
const GRAVITY = 0.45;
const JUMP_VELOCITY = -7;
const PLAYER_X = 54;
const PLAYER_RADIUS = 10;
const OBSTACLE_WIDTH = 34;
const OBSTACLE_GAP = 120;
const OBSTACLE_SPEED = 2.6;
const OBSTACLE_SPACING_FRAMES = 95;
const ROUND_SECONDS = 20;

function freshState() {
  return {
    playerY: HEIGHT / 2,
    velocity: 0,
    obstacles: [], // { x, gapY }
    frame: 0,
    score: 0,
    over: false,
  };
}

/** A quick tap-to-jump reflex break between reel cards - runs for up to ~20 seconds or until a collision. */
function TapDashGame({ onComplete }) {
  const canvasRef = useRef(null);
  const stateRef = useRef(freshState());
  const rafRef = useRef(null);
  const startRef = useRef(null);
  const [phase, setPhase] = useState("playing"); // playing | over | timeUp
  const [score, setScore] = useState(0);

  function jump() {
    if (phase !== "playing") return;
    stateRef.current.velocity = JUMP_VELOCITY;
  }

  useEffect(() => {
    const canvas = canvasRef.current;
    const ctx = canvas?.getContext("2d");
    if (!ctx) return undefined;

    startRef.current = Date.now();

    function tick() {
      const s = stateRef.current;

      if (!s.over) {
        s.frame += 1;
        s.velocity += GRAVITY;
        s.playerY += s.velocity;

        if (s.frame % OBSTACLE_SPACING_FRAMES === 0) {
          const gapY = 50 + Math.random() * (HEIGHT - 100 - OBSTACLE_GAP);
          s.obstacles.push({ x: WIDTH, gapY, scored: false });
        }
        s.obstacles.forEach((o) => {
          o.x -= OBSTACLE_SPEED;
        });
        s.obstacles = s.obstacles.filter((o) => o.x + OBSTACLE_WIDTH > 0);

        const hitWall = s.playerY - PLAYER_RADIUS < 0 || s.playerY + PLAYER_RADIUS > HEIGHT;
        const hitObstacle = s.obstacles.some((o) => {
          const withinX = PLAYER_X + PLAYER_RADIUS > o.x && PLAYER_X - PLAYER_RADIUS < o.x + OBSTACLE_WIDTH;
          if (!withinX) return false;
          const withinGap = s.playerY - PLAYER_RADIUS > o.gapY && s.playerY + PLAYER_RADIUS < o.gapY + OBSTACLE_GAP;
          return !withinGap;
        });

        s.obstacles.forEach((o) => {
          if (!o.scored && o.x + OBSTACLE_WIDTH < PLAYER_X) {
            o.scored = true;
            s.score += 1;
          }
        });

        if (hitWall || hitObstacle) {
          s.over = true;
          setPhase("over");
        } else if (Date.now() - startRef.current > ROUND_SECONDS * 1000) {
          s.over = true;
          setPhase("timeUp");
        }
        setScore(s.score);
      }

      ctx.clearRect(0, 0, WIDTH, HEIGHT);
      ctx.fillStyle = "#fff0eb";
      ctx.fillRect(0, 0, WIDTH, HEIGHT);

      ctx.fillStyle = "#39b4a0";
      s.obstacles.forEach((o) => {
        ctx.fillRect(o.x, 0, OBSTACLE_WIDTH, o.gapY);
        ctx.fillRect(o.x, o.gapY + OBSTACLE_GAP, OBSTACLE_WIDTH, HEIGHT - (o.gapY + OBSTACLE_GAP));
      });

      ctx.fillStyle = "#ff795b";
      ctx.beginPath();
      ctx.arc(PLAYER_X, s.playerY, PLAYER_RADIUS, 0, Math.PI * 2);
      ctx.fill();

      rafRef.current = requestAnimationFrame(tick);
    }

    rafRef.current = requestAnimationFrame(tick);
    return () => {
      if (rafRef.current) cancelAnimationFrame(rafRef.current);
    };
  }, []);

  useEffect(() => {
    function handleKey(event) {
      if (event.code === "Space") {
        event.preventDefault();
        jump();
      }
    }
    window.addEventListener("keydown", handleKey);
    return () => window.removeEventListener("keydown", handleKey);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [phase]);

  return (
    <div className="spark-game spark-game-dash">
      <p className="spark-game-instructions">Quick reflex round: tap to stay in the gap.</p>

      <div className="spark-dash-canvas-wrap" onClick={jump} role="presentation">
        <canvas ref={canvasRef} width={WIDTH} height={HEIGHT} className="spark-dash-canvas" />
        {phase !== "playing" && (
          <div className="spark-dash-overlay">
            <strong>{phase === "over" ? "Nice try!" : "Time's up!"}</strong>
            <span>Score: {score}</span>
          </div>
        )}
      </div>

      <div className="spark-game-footer">
        <span>{phase === "playing" ? `Score: ${score}` : "Round complete"}</span>
        <button type="button" className="button button-small" onClick={onComplete}>
          {phase === "playing" ? "Skip" : "Continue"}
        </button>
      </div>
    </div>
  );
}

export default TapDashGame;
